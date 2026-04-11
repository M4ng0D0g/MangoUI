package com.myudog.myulib.api.timer;

public final class TimerEvents {
    private TimerEvents() {
    }

    public record TimerStartedEvent(TimerModels.TimerSnapshot snapshot) {
    }

    public record TimerPausedEvent(TimerModels.TimerSnapshot snapshot) {
    }

    public record TimerResumedEvent(TimerModels.TimerSnapshot snapshot) {
    }

    public record TimerResetEvent(TimerModels.TimerSnapshot snapshot) {
    }

    public record TimerStoppedEvent(TimerModels.TimerSnapshot snapshot) {
    }

    public record TimerTickEvent(TimerModels.TimerSnapshot snapshot) {
    }

    public record TimerCheckpointEvent(TimerModels.TimerSnapshot snapshot, int bindingId, TimerModels.TimerTickBasis basis, long tick) {
    }

    public record TimerCompletedEvent(TimerModels.TimerSnapshot snapshot) {
    }
}
