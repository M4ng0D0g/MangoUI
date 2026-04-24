package com.myudog.myulib.api.command;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.game.core.GameConfig;
import com.myudog.myulib.api.game.core.GameData;
import com.myudog.myulib.api.game.core.GameDefinition;
import com.myudog.myulib.api.game.core.GameInstance;
import com.myudog.myulib.api.game.core.GameManager;
import com.myudog.myulib.api.game.state.BasicGameStateMachine;
import com.myudog.myulib.api.game.state.GameState;
import com.myudog.myulib.api.game.state.GameStateMachine;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.internal.event.EventDispatcherImpl;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class AccessCommandServiceTest {
    private enum TestState implements GameState {
        WAITING
    }

    private static final class TestData extends GameData {
    }

    private static final class TestDefinition extends GameDefinition<GameConfig, TestData, TestState> {
        private TestDefinition(Identifier id) {
            super(id);
        }

        @Override
        public TestData createInitialData(GameConfig config) {
            return new TestData();
        }

        @Override
        public GameStateMachine<TestState> createStateMachine(GameConfig config) {
            return new BasicGameStateMachine<>(TestState.WAITING, Map.of(TestState.WAITING, Set.of(TestState.WAITING)));
        }

        @Override
        protected EventDispatcherImpl createEventBus() {
            return new EventDispatcherImpl();
        }

        @Override
        protected void bindBehavior(GameInstance<GameConfig, TestData, TestState> instance) {
        }

        @Override
        protected void unbindBehavior(GameInstance<GameConfig, TestData, TestState> instance) {
        }
    }

    private static final class StubGameInstance extends GameInstance<GameConfig, TestData, TestState> {
        private int id;
        private boolean enabled;
        private boolean initialized;
        private boolean started;

        private StubGameInstance() {
            super(
                    0,
                    null,
                    null,
                    GameConfig.empty(),
                    new BasicGameStateMachine<>(TestState.WAITING, Map.of()),
                    new EventDispatcherImpl()
            );
        }

        private static StubGameInstance allocate(int id) {
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                Object unsafe = theUnsafe.get(null);
                StubGameInstance instance = (StubGameInstance) unsafeClass.getMethod("allocateInstance", Class.class)
                        .invoke(unsafe, StubGameInstance.class);
                instance.id = id;
                instance.enabled = true;
                instance.initialized = false;
                instance.started = false;
                return instance;
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to allocate StubGameInstance", ex);
            }
        }

        private void setInitialized(boolean initialized) {
            this.initialized = initialized;
        }

        @Override
        public int getInstanceId() {
            return id;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public boolean isStarted() {
            return started;
        }

        @Override
        public boolean shutdown() {
            if (!enabled) {
                return false;
            }
            enabled = false;
            started = false;
            return true;
        }
    }

    @TempDir
    Path tempDir;

    @SuppressWarnings("unchecked")
    private static Map<Integer, GameInstance<?, ?, ?>> instanceMap() {
        try {
            Field instances = GameManager.class.getDeclaredField("INSTANCES");
            instances.setAccessible(true);
            return (Map<Integer, GameInstance<?, ?, ?>>) instances.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to access GameManager instances map", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> tokenMap() {
        try {
            Field tokens = GameManager.class.getDeclaredField("INSTANCE_TOKENS");
            tokens.setAccessible(true);
            return (Map<String, Integer>) tokens.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to access GameManager token map", ex);
        }
    }

    @Test
    void accessServiceHelpersCreateAndPersistAccessData() {
        RoleGroupManager.INSTANCE.clear();
        PermissionManager.INSTANCE.clear();
        FieldManager.INSTANCE.clear();
        CommandRegistry.clear();
        assertDoesNotThrow(AccessCommandService::registerDefaults,
                "registerDefaults should only attach the command callback");
        assertTrue(CommandRegistry.snapshot().containsKey("myulib:save"),
                "registerDefaults should register the /myulib: local command mirror");
        Identifier builderId = Identifier.fromNamespaceAndPath("myulib", "builders");
        AccessCommandService.createRoleGroup(builderId, Component.literal("Builders"), 7);
        assertEquals("Builders", RoleGroupManager.INSTANCE.get(builderId).translationKey().getString(),
                "createRoleGroup should register the new group");
        AccessCommandService.grantGlobalPermission("builders", PermissionAction.BLOCK_PLACE, PermissionDecision.ALLOW);
        assertEquals(PermissionDecision.ALLOW,
                PermissionManager.INSTANCE.global().forGroup("builders").get(PermissionAction.BLOCK_PLACE),
                "grantGlobalPermission should update the global permission table");
        Identifier fieldId = Identifier.fromNamespaceAndPath("tests", "spawn");
        FieldDefinition field = new FieldDefinition(
                fieldId,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"),
                new AABB(0, 0, 0, 10, 10, 10),
                Map.of("label", "Spawn")
        );
        AccessCommandService.createField(field);
        assertEquals(field, FieldManager.INSTANCE.get(fieldId), "createField should register the field");
        assertTrue(AccessCommandService.listRoleGroups().stream().anyMatch(group -> group.id().equals(builderId)),
                "listRoleGroups should include the created group");
        AccessCommandService.deleteField(fieldId);
        AccessCommandService.deleteRoleGroup(builderId);
        assertNull(FieldManager.INSTANCE.get(fieldId), "deleteField should remove the field");
        assertNull(RoleGroupManager.INSTANCE.get(builderId), "deleteRoleGroup should remove the role group");

        CommandResult save = CommandRegistry.execute(new CommandContext("console", "myulib:save", Map.of()));
        assertTrue(save.success(), "The mirrored myulib:save command should execute successfully");
        CommandResult status = CommandRegistry.execute(new CommandContext("console", "myulib:status", Map.of()));
        assertTrue(status.success(), "The mirrored myulib:status command should execute successfully");
        assertTrue(status.message().contains("field="), "Status output should include field count");
    }

    @Test
    void localGameReadAndEndCommandsExposeLifecycleState() {
        CommandRegistry.clear();
        AccessCommandService.registerDefaults();

        String token = "command_end_room";
        int instanceId = 42_001;
        StubGameInstance instance = StubGameInstance.allocate(instanceId);
        Map<Integer, GameInstance<?, ?, ?>> instances = instanceMap();
        Map<String, Integer> tokens = tokenMap();
        try {
            instances.put(instanceId, instance);
            tokens.put(token, instanceId);

            CommandResult readBeforeInit = CommandRegistry.execute(new CommandContext("console", "myulib:game:read", Map.of("id", token)));
            assertTrue(readBeforeInit.success(), "myulib:game:read should succeed for an existing instance");
            assertTrue(readBeforeInit.message().contains("initialized:false"), "Read output should expose initialized=false before init");

            instance.setInitialized(true);

            CommandResult readAfterInit = CommandRegistry.execute(new CommandContext("console", "myulib:game:read", Map.of("id", token)));
            assertTrue(readAfterInit.success(), "myulib:game:read should still succeed after init");
            assertTrue(readAfterInit.message().contains("initialized:true"), "Read output should expose initialized=true after init");

            CommandResult endResult = CommandRegistry.execute(new CommandContext("console", "myulib:game:end", Map.of("id", token)));
            assertTrue(endResult.success(), "myulib:game:end should end an existing instance");
            assertEquals("game=ended:" + instanceId, endResult.message(), "End output should include ended instance id");

            CommandResult readAfterEnd = CommandRegistry.execute(new CommandContext("console", "myulib:game:read", Map.of("id", token)));
            assertTrue(readAfterEnd.success(), "myulib:game:read should still resolve after end for recycled instances");
            assertTrue(readAfterEnd.message().contains("enabled:false"), "Read after end should expose disabled/recycled lifecycle state");

            CommandResult endAgain = CommandRegistry.execute(new CommandContext("console", "myulib:game:end", Map.of("id", token)));
            assertFalse(endAgain.success(), "myulib:game:end should fail when the instance is no longer endable");
            assertEquals("game=end_failed_or_not_found", endAgain.message(), "Repeated end should report end_failed_or_not_found");
        } finally {
            instances.remove(instanceId);
            tokens.remove(token);
        }
    }
}
