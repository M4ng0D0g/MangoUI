package com.myudog.myulib.api.timer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TimerModels {
    private TimerModels() {
    }

    public enum TimerMode { COUNT_UP, COUNT_DOWN }
    public enum TimerStatus { IDLE, RUNNING, PAUSED, STOPPED, COMPLETED }
    public enum TimerTickBasis { ELAPSED, REMAINING }

    public interface TimerPayload {
    }

    public record RespawnTimerPayload(UUID playerId, boolean allowSkip) implements TimerPayload {
    }

    public record SoundTimerPayload(String soundId, float volume, float pitch) implements TimerPayload {
    }

    @FunctionalInterface
    public interface TimerAction {
        void invoke(TimerSnapshot snapshot);
    }

    public record TimerBinding(int id, long tick, TimerTickBasis basis, TimerAction action) {
    }

    public record TimerSnapshot(int timerEntityId, Long ownerEntityId, Timer timer, TimerStatus status, long elapsedTicks, long remainingTicks, TimerPayload payload, long currentTick) {
        public long durationTicks() { return timer == null ? 0L : timer.durationTicks; }
        public TimerMode mode() { return timer == null ? TimerMode.COUNT_UP : timer.mode; }
        public double progress() { long duration = Math.max(1L, durationTicks()); return Math.min(1.0d, Math.max(0.0d, (double) elapsedTicks / duration)); }
        @SuppressWarnings("unchecked") public <T extends TimerPayload> T payloadAs() { return (T) payload; }
        public <T extends TimerPayload> T requirePayload() { if (payload == null) throw new IllegalStateException("Timer payload is missing"); return payloadAs(); }
    }

    public static final class Timer {
        public final net.minecraft.resources.Identifier id;
        public final long durationTicks;
        public final TimerMode mode;
        public final boolean autoStopOnComplete;
        public final Map<Integer, TimerBinding> elapsedBindings = new LinkedHashMap<>();
        public final Map<Integer, TimerBinding> remainingBindings = new LinkedHashMap<>();
        public final List<TimerAction> startedActions = new ArrayList<>();
        public final List<TimerAction> pausedActions = new ArrayList<>();
        public final List<TimerAction> resumedActions = new ArrayList<>();
        public final List<TimerAction> resetActions = new ArrayList<>();
        public final List<TimerAction> stoppedActions = new ArrayList<>();
        public final List<TimerAction> completedActions = new ArrayList<>();
        private int nextBindingId = 1;

        public Timer(net.minecraft.resources.Identifier id, long durationTicks) { this(id, durationTicks, TimerMode.COUNT_UP, true); }
        public Timer(net.minecraft.resources.Identifier id, long durationTicks, TimerMode mode, boolean autoStopOnComplete) {
            this.id = Objects.requireNonNull(id, "id");
            this.durationTicks = Math.max(0L, durationTicks);
            this.mode = mode == null ? TimerMode.COUNT_UP : mode;
            this.autoStopOnComplete = autoStopOnComplete;
        }

        public Timer onElapsedTick(long tick, TimerAction action) { return onElapsedTick(tick, action, false); }
        public Timer onRemainingTick(long tick, TimerAction action) { return onRemainingTick(tick, action, false); }
        public Timer onElapsedTick(long tick, TimerAction action, boolean replace) { return addBinding(elapsedBindings, tick, TimerTickBasis.ELAPSED, action, replace); }
        public Timer onRemainingTick(long tick, TimerAction action, boolean replace) { return addBinding(remainingBindings, tick, TimerTickBasis.REMAINING, action, replace); }
        public Timer onStarted(TimerAction action) { startedActions.add(action); return this; }
        public Timer onPaused(TimerAction action) { pausedActions.add(action); return this; }
        public Timer onResumed(TimerAction action) { resumedActions.add(action); return this; }
        public Timer onReset(TimerAction action) { resetActions.add(action); return this; }
        public Timer onStopped(TimerAction action) { stoppedActions.add(action); return this; }
        public Timer onCompleted(TimerAction action) { completedActions.add(action); return this; }
        public boolean removeBinding(int bindingId) { return elapsedBindings.remove(bindingId) != null || remainingBindings.remove(bindingId) != null; }

        private Timer addBinding(Map<Integer, TimerBinding> bindings, long tick, TimerTickBasis basis, TimerAction action, boolean replace) {
            int id = nextBindingId++;
            if (!replace && bindings.values().stream().anyMatch(binding -> binding.tick() == tick && binding.basis() == basis)) {
                return this;
            }
            bindings.put(id, new TimerBinding(id, tick, basis, action));
            return this;
        }
    }

    public static final class TimerInstance {
        public final int timerEntityId;
        public final net.minecraft.resources.Identifier timerId;
        public final Long ownerEntityId;
        public TimerPayload payload;
        public TimerStatus status;
        public long elapsedTicks;
        public long lastUpdatedTick;
        public long pausedTicks;

        public TimerInstance(int timerEntityId, net.minecraft.resources.Identifier timerId, Long ownerEntityId, TimerPayload payload) {
            this.timerEntityId = timerEntityId;
            this.timerId = timerId;
            this.ownerEntityId = ownerEntityId;
            this.payload = payload;
            this.status = TimerStatus.IDLE;
        }

        public boolean isRunning() { return status == TimerStatus.RUNNING; }
        public boolean isPaused() { return status == TimerStatus.PAUSED; }
        public boolean isStopped() { return status == TimerStatus.STOPPED; }
        public boolean isCompleted() { return status == TimerStatus.COMPLETED; }
    }
}

