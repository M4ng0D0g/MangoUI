package com.myudog.myulib.api.game.logic;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.game.timer.TimerModels.TimerSnapshot;

public final class LogicSignals {
    private LogicSignals() {
    }

    public record GameInstanceCreatedSignal<S extends GameState>(GameInstance<S> instance) implements LogicContracts.LogicSignal {
    }

    public record GameInstanceDestroyedSignal<S extends GameState>(GameInstance<S> instance) implements LogicContracts.LogicSignal {
    }

    public record GameStateChangedSignal<S extends GameState>(GameInstance<S> instance, S previousState, S currentState) implements LogicContracts.LogicSignal {
    }

    public record TimerStartedSignal(TimerSnapshot snapshot) implements LogicContracts.LogicSignal {
    }

    public record TimerPausedSignal(TimerSnapshot snapshot) implements LogicContracts.LogicSignal {
    }

    public record TimerResumedSignal(TimerSnapshot snapshot) implements LogicContracts.LogicSignal {
    }

    public record TimerResetSignal(TimerSnapshot snapshot) implements LogicContracts.LogicSignal {
    }

    public record TimerStoppedSignal(TimerSnapshot snapshot) implements LogicContracts.LogicSignal {
    }

    public record TimerTickSignal(TimerSnapshot snapshot) implements LogicContracts.LogicSignal {
    }

    public record TimerCheckpointSignal(TimerSnapshot snapshot, int bindingId, com.myudog.myulib.api.game.timer.TimerModels.TimerTickBasis basis, long tick) implements LogicContracts.LogicSignal {
    }

    public record TimerCompletedSignal(TimerSnapshot snapshot) implements LogicContracts.LogicSignal {
    }
}
