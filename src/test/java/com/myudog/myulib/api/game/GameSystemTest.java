package com.myudog.myulib.api.game;
import com.myudog.myulib.api.game.core.GameConfig;
import com.myudog.myulib.api.game.core.GameData;
import com.myudog.myulib.api.game.core.GameDefinition;
import com.myudog.myulib.api.game.core.GameInstance;
import com.myudog.myulib.api.game.object.GameObjectKind;
import com.myudog.myulib.api.game.state.BasicGameStateMachine;
import com.myudog.myulib.api.game.state.GameState;
import com.myudog.myulib.api.game.state.GameStateChangeEvent;
import com.myudog.myulib.api.game.state.GameStateMachine;
import com.myudog.myulib.internal.event.EventDispatcherImpl;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class GameSystemTest {
    private enum TestState implements GameState {
        PREPARE,
        ACTIVE,
        FINISHED
    }
    private static final class TestGameData extends GameData {
    }
    private static final class TestGameDefinition extends GameDefinition<GameConfig, TestGameData, TestState> {
        private TestGameDefinition(Identifier id) {
            super(id);
        }
        @Override
        public TestGameData createInitialData(GameConfig config) {
            return new TestGameData();
        }
        @Override
        public GameStateMachine<TestState> createStateMachine(GameConfig config) {
            return new BasicGameStateMachine<>(
                    TestState.PREPARE,
                    Map.of(
                            TestState.PREPARE, Set.of(TestState.ACTIVE),
                            TestState.ACTIVE, Set.of(TestState.FINISHED)
                    )
            );
        }
        @Override
        protected EventDispatcherImpl createEventBus() {
            return new EventDispatcherImpl();
        }
        @Override
        public void bindBehaviors(GameInstance<GameConfig, TestGameData, TestState> instance) {
        }
    }
    @Test
    void coreGameTypesAndStateMachineBehaveAsExpected() {
        GameConfig config = GameConfig.empty();
        assertDoesNotThrow(config::validate, "Empty game config should validate successfully");
        assertTrue(config.gameObjects().isEmpty(), "Empty game config should not contain game objects");
        assertTrue(config.metadata().isEmpty(), "Empty game config should not contain metadata");
        TestGameData data = new TestGameData();
        data.timerInstanceIds().add(7);
        data.timerTags().put(7, "respawn");
        data.scoreboardLines().add("score");
        data.scoreboardValues().put("kills", 3);
        data.reset();
        assertTrue(data.timerInstanceIds().isEmpty(), "GameData.reset should clear timer ids");
        assertTrue(data.timerTags().isEmpty(), "GameData.reset should clear timer tags");
        assertTrue(data.scoreboardLines().isEmpty(), "GameData.reset should clear scoreboard lines");
        assertTrue(data.scoreboardValues().isEmpty(), "GameData.reset should clear scoreboard values");
        assertEquals(List.of(
                GameObjectKind.RESPAWN_POINT,
                GameObjectKind.MINEABLE,
                GameObjectKind.USABLE,
                GameObjectKind.ATTACKABLE,
                GameObjectKind.INTERACTABLE,
                GameObjectKind.CUSTOM
        ), List.of(GameObjectKind.values()), "GameObjectKind should expose the current six kinds in declaration order");
        BasicGameStateMachine<TestState> machine = new BasicGameStateMachine<>(
                TestState.PREPARE,
                Map.of(
                        TestState.PREPARE, Set.of(TestState.ACTIVE),
                        TestState.ACTIVE, Set.of(TestState.FINISHED)
                )
        );
        assertEquals(TestState.PREPARE, machine.getCurrent(), "State machine should start at the initial state");
        assertTrue(machine.canTransition(TestState.ACTIVE), "State machine should allow the configured transition");
        assertFalse(machine.canTransition(TestState.FINISHED), "State machine should reject an unconfigured transition");
        assertTrue(machine.transitionTo(TestState.ACTIVE), "Configured transition should succeed");
        assertEquals(TestState.ACTIVE, machine.getCurrent(), "Current state should update after a successful transition");
        machine.reset();
        assertEquals(TestState.PREPARE, machine.getCurrent(), "Reset should return the state machine to the initial state");
    }
    @Test
    void gameDefinitionAndManagerCreateRunnableInstances() {
        Identifier gameId = Identifier.fromNamespaceAndPath("tests", "arena");
        TestGameDefinition definition = new TestGameDefinition(gameId);
        GameInstance<GameConfig, TestGameData, TestState> instance = null;
        int instanceId = -1;
        try {
            GameManager.register(definition);
            assertEquals(definition, GameManager.definition(gameId), "Registered definition should be retrievable");
            assertTrue(GameManager.hasDefinition(gameId), "GameManager should report the registered definition");
            instance = GameManager.createInstance(gameId, GameConfig.empty());
            instanceId = instance.getInstanceId();
            assertEquals(TestState.PREPARE, instance.getCurrentState(), "New instance should start in the initial state");
            assertEquals(0L, instance.getTickCount(), "New instance should start with zero ticks");
            instance.tick();
            assertEquals(1L, instance.getTickCount(), "Tick should increment the instance tick counter");
            assertTrue(instance.canTransition(TestState.ACTIVE), "Instance should allow the configured transition to ACTIVE");
            assertTrue(instance.transition(TestState.ACTIVE), "Transition to ACTIVE should succeed");
            assertEquals(TestState.ACTIVE, instance.getCurrentState(), "Current state should update after a valid transition");
            GameStateChangeEvent<TestState> event = new GameStateChangeEvent<>(instance, TestState.PREPARE, TestState.ACTIVE);
            assertTrue(event.isEntering(TestState.ACTIVE), "GameStateChangeEvent should report entering the target state");
            assertTrue(event.isLeaving(TestState.PREPARE), "GameStateChangeEvent should report leaving the source state");
            assertEquals(1, GameManager.getInstances(gameId).size(), "GameManager should report exactly one instance for the registered game");
            assertTrue(GameManager.destroyInstance(instanceId), "Destroying the instance should succeed");
            assertNull(GameManager.getInstance(instanceId), "Destroyed instance should no longer be retrievable");
        } finally {
            if (instanceId != -1) {
                GameManager.destroyInstance(instanceId);
            }
            GameManager.unregister(gameId);
        }
    }
}
