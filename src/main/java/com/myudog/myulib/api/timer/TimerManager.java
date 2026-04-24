package com.myudog.myulib.api.timer;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class TimerManager {

    public static final TimerManager INSTANCE = new TimerManager();

    
    private final Map<Identifier, TimerDefinition> TIMERS = new LinkedHashMap<>();
    private final Map<Integer, TimerInstance> INSTANCES = new ConcurrentHashMap<>();
    private final AtomicInteger NEXT_INSTANCE_ID = new AtomicInteger(1);

    private TimerManager() {
    }

    public void install() {
        // 1. 掛載伺服器 Tick 事件 (驅動 Timer 的核心引擎)
        // 呼叫我們之前優化過的 update 方法
        ServerTickEvents.END_SERVER_TICK.register(TimerManager.INSTANCE::update);

        // 2. 掛載伺服器停止事件 (安全機制)
        // 伺服器關閉時，清空所有的實例與藍圖，防止重啟或 /reload 時發生記憶體洩漏
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            TIMERS.clear();
            INSTANCES.clear();
            NEXT_INSTANCE_ID.set(1);
            System.out.println("[Myulib] TimerManager 已成功釋放！");

        });

        // 可選：印出啟動訊息方便 Debug
        System.out.println("[Myulib] TimerManager 事件已成功掛載！");
    }

    public void register(TimerDefinition timer) {
        if (!validate(timer)) {
            throw new IllegalArgumentException("TimerDefinition 驗證失敗: " + (timer == null ? "null" : timer.id));
        }
        TIMERS.put(timer.id, timer);
        DebugLogManager.INSTANCE.log(DebugFeature.TIMER,
                "register id=" + timer.id + ",duration=" + timer.durationTicks + ",mode=" + timer.mode);
    }

    public boolean validate(TimerDefinition timer) {
        return timer != null && timer.id != null && !TIMERS.containsKey(timer.id);
    }

    public TimerDefinition unregister(Identifier timerId) {
        DebugLogManager.INSTANCE.log(DebugFeature.TIMER, "unregister id=" + timerId);
        return TIMERS.remove(timerId);
    }

    public boolean has(Identifier timerId) {
        return TIMERS.containsKey(timerId);
    }

    public int createInstance(Identifier timerId, Long ownerEntityId, TimerPayload payload, boolean autoStart, Object Level) {
        TimerDefinition timer = TIMERS.get(timerId);
        if (timer == null) {
            throw new IllegalArgumentException("Unknown timer: " + timerId);
        }
        int instanceId = NEXT_INSTANCE_ID.getAndIncrement();
        TimerInstance instance = new TimerInstance(instanceId, timerId, ownerEntityId, payload);
        INSTANCES.put(instanceId, instance);
        DebugLogManager.INSTANCE.log(DebugFeature.TIMER,
                "create instance=" + instanceId + ",timer=" + timerId + ",owner=" + ownerEntityId + ",autoStart=" + autoStart);
        if (autoStart) {
            start(instanceId);
        }
        return instanceId;
    }

    public TimerInstance getInstance(int timerEntityId, Object Level) {
        return INSTANCES.get(timerEntityId);
    }

    public TimerSnapshot getSnapshot(int timerEntityId) {
        TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance == null) {
            return null;
        }
        TimerDefinition timer = TIMERS.get(instance.timerId);
        if (timer == null) {
            return null;
        }
        long remaining = Math.max(0L, timer.durationTicks - instance.elapsedTicks);
        return new TimerSnapshot(timerEntityId, instance.ownerEntityId, timer, instance.status, instance.elapsedTicks, remaining, instance.payload, instance.lastUpdatedTick);
    }

    public List<TimerInstance> findInstances(Long ownerEntityId, Object Level) {
        return INSTANCES.values().stream().filter(instance -> Objects.equals(instance.ownerEntityId, ownerEntityId)).toList();
    }

    public boolean isRunning(int timerEntityId, Object Level) { return getInstance(timerEntityId, Level) != null && getInstance(timerEntityId, Level).isRunning(); }
    public boolean isPaused(int timerEntityId, Object Level) { return getInstance(timerEntityId, Level) != null && getInstance(timerEntityId, Level).isPaused(); }
    public boolean isStopped(int timerEntityId, Object Level) { return getInstance(timerEntityId, Level) != null && getInstance(timerEntityId, Level).isStopped(); }
    public boolean isCompleted(int timerEntityId, Object Level) { return getInstance(timerEntityId, Level) != null && getInstance(timerEntityId, Level).isCompleted(); }

    public void start(int timerEntityId) {
        updateState(timerEntityId, TimerStatus.RUNNING, snapshot -> snapshot.timer().startedActions.forEach(action -> action.invoke(snapshot)));
    }

    public int start(long ticks, Consumer<Integer> onExpire) {
        Identifier timerId = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "auto_timer_" + NEXT_INSTANCE_ID.get());
        TimerDefinition timer = new TimerDefinition(timerId, ticks, TimerMode.COUNT_DOWN, true)
                .onCompleted(snapshot -> {
                    if (onExpire != null) {
                        onExpire.accept(snapshot.timerEntityId());
                    }
                    unregister(timerId);
                });
        register(timer);
        return createInstance(timerId, null, null, true, null);
    }

    public void pause(int timerEntityId) {
        updateState(timerEntityId, TimerStatus.PAUSED, snapshot -> snapshot.timer().pausedActions.forEach(action -> action.invoke(snapshot)));
    }

    public void resume(int timerEntityId) {
        updateState(timerEntityId, TimerStatus.RUNNING, snapshot -> snapshot.timer().resumedActions.forEach(action -> action.invoke(snapshot)));
    }

    public void stop(int timerEntityId) {
        updateState(timerEntityId, TimerStatus.STOPPED, snapshot -> snapshot.timer().stoppedActions.forEach(action -> action.invoke(snapshot)));
    }

    public void reset(int timerEntityId, boolean clearPayload) {
        TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance == null) return;
        instance.elapsedTicks = 0;
        instance.lastUpdatedTick = 0;
        instance.pausedTicks = 0;
        instance.status = TimerStatus.IDLE;
        if (clearPayload) {
            instance.payload = null;
        }
        DebugLogManager.INSTANCE.log(DebugFeature.TIMER,
                "reset instance=" + timerEntityId + ",clearPayload=" + clearPayload);
    }

    public void setElapsedTicks(int timerEntityId, long ticks) {
        TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance != null) {
            instance.elapsedTicks = Math.max(0L, ticks);
        }
    }

    public void setRemainingTicks(int timerEntityId, long ticks) {
        TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance != null) {
            TimerDefinition timer = TIMERS.get(instance.timerId);
            if (timer != null) {
                setElapsedTicks(timerEntityId, Math.max(0L, timer.durationTicks - ticks));
            }
        }
    }

    public void setPayload(int timerEntityId, TimerPayload payload) {
        TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance != null) {
            instance.payload = payload;
        }
    }

    public int timerDefinitionCount() {
        return TIMERS.size();
    }

    public int timerInstanceCount() {
        return INSTANCES.size();
    }


    public void update(MinecraftServer server) { // 建議將 Object Level 改為 MinecraftServer
        // 使用迭代器以便在遍歷時安全刪除已完成的實例
        java.util.Iterator<Map.Entry<Integer, TimerInstance>> iterator = INSTANCES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, TimerInstance> entry = iterator.next();
            int id = entry.getKey();
            TimerInstance instance = entry.getValue();

            // 如果計時器已經停止或完成，直接從記憶體中移除以防止洩漏！
            if (instance.isStopped() || instance.isCompleted()) {
                iterator.remove();
                continue;
            }

            if (!instance.isRunning()) {
                continue;
            }

            instance.elapsedTicks++;
            instance.lastUpdatedTick++;
            TimerDefinition timer = TIMERS.get(instance.timerId);

            if (timer == null) continue;

            TimerSnapshot snapshot = getSnapshot(id);
            if (snapshot != null) {
                long remaining = Math.max(0L, timer.durationTicks - instance.elapsedTicks);

                // 🚀 效能優化：捨棄 Stream，改用傳統 foreach 避免每 Tick 產生大量 GC 垃圾
                for (TimerBinding binding : timer.elapsedBindings.values()) {
                    if (binding.basis() == TimerTickBasis.ELAPSED && binding.matches(instance.elapsedTicks)) {
                        binding.action().invoke(snapshot);
                    }
                }

                for (TimerBinding binding : timer.remainingBindings.values()) {
                    if (binding.basis() == TimerTickBasis.REMAINING && binding.matches(remaining)) {
                        binding.action().invoke(snapshot);
                    }
                }

                // 檢查是否完成
                if (instance.elapsedTicks >= timer.durationTicks) {
                    instance.status = TimerStatus.COMPLETED;
                    timer.completedActions.forEach(action -> action.invoke(snapshot));
                    DebugLogManager.INSTANCE.log(DebugFeature.TIMER,
                            "completed instance=" + id + ",timer=" + instance.timerId + ",elapsed=" + instance.elapsedTicks);

                    if (timer.autoStopOnComplete) {
                        instance.status = TimerStatus.STOPPED;
                    }
                }
            }
        }
    }

    private void updateState(int timerEntityId, TimerStatus status, java.util.function.Consumer<TimerSnapshot> actionConsumer) {
        TimerInstance instance = INSTANCES.get(timerEntityId);
        if (instance == null) {
            return;
        }
        instance.status = status;
        DebugLogManager.INSTANCE.log(DebugFeature.TIMER,
                "state instance=" + timerEntityId + " -> " + status);
        TimerSnapshot snapshot = getSnapshot(timerEntityId);
        if (snapshot != null) {
            actionConsumer.accept(snapshot);
        }
    }
}


