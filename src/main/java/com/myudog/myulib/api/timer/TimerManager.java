package com.myudog.myulib.api.timer;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerManager {
    private static final Map<Identifier, TimerModels.Timer> TIMERS = new LinkedHashMap<>();
    private static final Map<Integer, TimerModels.TimerInstance> INSTANCES = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_INSTANCE_ID = new AtomicInteger(1);

    private TimerManager() {
    }

    public static void install() {
    }

    public static void register(TimerModels.Timer timer) {
        TIMERS.put(timer.id, timer);
    }

    public static TimerModels.Timer unregister(Identifier timerId) {
        return TIMERS.remove(timerId);
    }

    public static boolean has(Identifier timerId) {
        return TIMERS.containsKey(timerId);
    }

    public static int createInstance(Identifier timerId, Long ownerEntityId, TimerModels.TimerPayload payload, boolean autoStart, Object Level) {
        TimerModels.Timer timer = TIMERS.get(timerId);
        if (timer == null) {
            throw new IllegalArgumentException("Unknown timer: " + timerId);
        }
        int instanceId = NEXT_INSTANCE_ID.getAndIncrement();
        TimerModels.TimerInstance instance = new TimerModels.TimerInstance(instanceId, timerId, ownerEntityId, payload);
        INSTANCES.put(instanceId, instance);
        if (autoStart) {
            start(instanceId);
        }
        return instanceId;
    }

    public static TimerModels.TimerInstance getInstance(int timerEntityId, Object Level) {
        return INSTANCES.get(timerEntityId);
    }

    public static TimerModels.TimerSnapshot getSnapshot(int timerEntityId) {
        TimerModels.TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance == null) {
            return null;
        }
        TimerModels.Timer timer = TIMERS.get(instance.timerId);
        if (timer == null) {
            return null;
        }
        long remaining = Math.max(0L, timer.durationTicks - instance.elapsedTicks);
        return new TimerModels.TimerSnapshot(timerEntityId, instance.ownerEntityId, timer, instance.status, instance.elapsedTicks, remaining, instance.payload, instance.lastUpdatedTick);
    }

    public static List<TimerModels.TimerInstance> findInstances(Long ownerEntityId, Object Level) {
        return INSTANCES.values().stream().filter(instance -> Objects.equals(instance.ownerEntityId, ownerEntityId)).toList();
    }

    public static boolean isRunning(int timerEntityId, Object Level) { return getInstance(timerEntityId, Level) != null && getInstance(timerEntityId, Level).isRunning(); }
    public static boolean isPaused(int timerEntityId, Object Level) { return getInstance(timerEntityId, Level) != null && getInstance(timerEntityId, Level).isPaused(); }
    public static boolean isStopped(int timerEntityId, Object Level) { return getInstance(timerEntityId, Level) != null && getInstance(timerEntityId, Level).isStopped(); }
    public static boolean isCompleted(int timerEntityId, Object Level) { return getInstance(timerEntityId, Level) != null && getInstance(timerEntityId, Level).isCompleted(); }

    public static void start(int timerEntityId) {
        updateState(timerEntityId, TimerModels.TimerStatus.RUNNING, snapshot -> snapshot.timer().startedActions.forEach(action -> action.invoke(snapshot)));
    }

    public static void pause(int timerEntityId) {
        updateState(timerEntityId, TimerModels.TimerStatus.PAUSED, snapshot -> snapshot.timer().pausedActions.forEach(action -> action.invoke(snapshot)));
    }

    public static void resume(int timerEntityId) {
        updateState(timerEntityId, TimerModels.TimerStatus.RUNNING, snapshot -> snapshot.timer().resumedActions.forEach(action -> action.invoke(snapshot)));
    }

    public static void stop(int timerEntityId) {
        updateState(timerEntityId, TimerModels.TimerStatus.STOPPED, snapshot -> snapshot.timer().stoppedActions.forEach(action -> action.invoke(snapshot)));
    }

    public static void reset(int timerEntityId, boolean clearPayload) {
        TimerModels.TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance == null) return;
        instance.elapsedTicks = 0;
        instance.lastUpdatedTick = 0;
        instance.pausedTicks = 0;
        instance.status = TimerModels.TimerStatus.IDLE;
        if (clearPayload) {
            instance.payload = null;
        }
    }

    public static void setElapsedTicks(int timerEntityId, long ticks) {
        TimerModels.TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance != null) {
            instance.elapsedTicks = Math.max(0L, ticks);
        }
    }

    public static void setRemainingTicks(int timerEntityId, long ticks) {
        TimerModels.TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance != null) {
            TimerModels.Timer timer = TIMERS.get(instance.timerId);
            if (timer != null) {
                setElapsedTicks(timerEntityId, Math.max(0L, timer.durationTicks - ticks));
            }
        }
    }

    public static void setPayload(int timerEntityId, TimerModels.TimerPayload payload) {
        TimerModels.TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance != null) {
            instance.payload = payload;
        }
    }

    public static void update(Object Level) {
        for (Map.Entry<Integer, TimerModels.TimerInstance> entry : INSTANCES.entrySet()) {
            int id = entry.getKey();
            TimerModels.TimerInstance instance = entry.getValue();
            if (!instance.isRunning()) {
                continue;
            }
            instance.elapsedTicks++;
            instance.lastUpdatedTick++;
            TimerModels.Timer timer = TIMERS.get(instance.timerId);
            if (timer == null) {
                continue;
            }
            TimerModels.TimerSnapshot snapshot = getSnapshot(id);
            if (snapshot != null) {
                timer.elapsedBindings.values().stream().filter(binding -> binding.basis() == TimerModels.TimerTickBasis.ELAPSED && binding.tick() == instance.elapsedTicks).forEach(binding -> binding.action().invoke(snapshot));
                long remaining = Math.max(0L, timer.durationTicks - instance.elapsedTicks);
                timer.remainingBindings.values().stream().filter(binding -> binding.basis() == TimerModels.TimerTickBasis.REMAINING && binding.tick() == remaining).forEach(binding -> binding.action().invoke(snapshot));
                if (instance.elapsedTicks >= timer.durationTicks) {
                    instance.status = TimerModels.TimerStatus.COMPLETED;
                    timer.completedActions.forEach(action -> action.invoke(snapshot));
                    if (timer.autoStopOnComplete) {
                        instance.status = TimerModels.TimerStatus.STOPPED;
                    }
                }
            }
        }
    }

    private static void updateState(int timerEntityId, TimerModels.TimerStatus status, java.util.function.Consumer<TimerModels.TimerSnapshot> actionConsumer) {
        TimerModels.TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance == null) {
            return;
        }
        instance.status = status;
        TimerModels.TimerSnapshot snapshot = getSnapshot(timerEntityId);
        if (snapshot != null) {
            actionConsumer.accept(snapshot);
        }
    }
}


