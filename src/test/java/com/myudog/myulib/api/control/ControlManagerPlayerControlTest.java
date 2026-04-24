package com.myudog.myulib.api.control;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ControlManagerPlayerControlTest {

    @AfterEach
    void cleanup() {
        ControlManager.INSTANCE.clearAllPlayerControls();
    }

    @Test
    void playerControlsDefaultToEnabledAndCanBeToggled() {
        UUID playerId = UUID.randomUUID();

        assertTrue(ControlManager.INSTANCE.isPlayerControlEnabled(playerId, ControlType.MOVE));
        assertTrue(ControlManager.INSTANCE.setPlayerControl(playerId, ControlType.MOVE, false));
        assertFalse(ControlManager.INSTANCE.isPlayerControlEnabled(playerId, ControlType.MOVE));

        Set<ControlType> disabled = ControlManager.INSTANCE.disabledPlayerControls(playerId);
        assertTrue(disabled.contains(ControlType.MOVE));

        assertTrue(ControlManager.INSTANCE.setPlayerControl(playerId, ControlType.MOVE, true));
        assertTrue(ControlManager.INSTANCE.isPlayerControlEnabled(playerId, ControlType.MOVE));
        assertTrue(ControlManager.INSTANCE.disabledPlayerControls(playerId).isEmpty());
    }

    @Test
    void controlTypeParseAcceptsLegacyPermissionNames() {
        assertEquals(ControlType.MOVE, ControlType.parse("move"));
        assertEquals(ControlType.SPRINT, ControlType.parse("PLAYER_SPRINT"));
        assertEquals(ControlType.ROTATE, ControlType.parse("player-look"));
        assertEquals(ControlType.SNEAK, ControlType.parse("sneak"));
        assertEquals(ControlType.JUMP, ControlType.parse("jump"));
    }
}


