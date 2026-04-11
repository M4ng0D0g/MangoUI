package com.myudog.myulib.api.ui;
import com.myudog.myulib.api.permission.ScopeLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
final class ConfigurationUiRegistryTest {
    @AfterEach
    void reset() {
        ConfigurationUiRegistry.setBridge(null);
    }
    @Test
    void registryForwardsCallsAndNoopBridgeStaysSilent() {
        RecordingBridge bridge = new RecordingBridge();
        ConfigurationUiRegistry.setBridge(bridge);
        ConfigurationUiRegistry.openFieldEditor("field");
        ConfigurationUiRegistry.openIdentityGroupEditor("identity");
        ConfigurationUiRegistry.openRoleGroupEditor("role");
        ConfigurationUiRegistry.openTeamEditor("team");
        ConfigurationUiRegistry.openPermissionEditor(ScopeLayer.FIELD, "scope");
        assertEquals(List.of(
                "field:field",
                "identity:identity",
                "role:role",
                "team:team",
                "permission:FIELD:scope"
        ), bridge.calls, "ConfigurationUiRegistry should forward calls in order");
        assertDoesNotThrow(() -> {
            NoopConfigurationUiBridge.INSTANCE.openFieldEditor("ignored");
            NoopConfigurationUiBridge.INSTANCE.openIdentityGroupEditor("ignored");
            NoopConfigurationUiBridge.INSTANCE.openRoleGroupEditor("ignored");
            NoopConfigurationUiBridge.INSTANCE.openTeamEditor("ignored");
            NoopConfigurationUiBridge.INSTANCE.openPermissionEditor(ScopeLayer.GLOBAL, "ignored");
        }, "NoopConfigurationUiBridge should ignore all calls without throwing");
    }
    private static final class RecordingBridge implements ConfigurationUiBridge {
        private final List<String> calls = new ArrayList<>();
        @Override
        public void openFieldEditor(String fieldId) {
            calls.add("field:" + fieldId);
        }
        @Override
        public void openIdentityGroupEditor(String groupId) {
            calls.add("identity:" + groupId);
        }
        @Override
        public void openRoleGroupEditor(String groupId) {
            calls.add("role:" + groupId);
        }
        @Override
        public void openTeamEditor(String teamId) {
            calls.add("team:" + teamId);
        }
        @Override
        public void openPermissionEditor(ScopeLayer layer, String scopeId) {
            calls.add("permission:" + layer + ":" + scopeId);
        }
    }
}
