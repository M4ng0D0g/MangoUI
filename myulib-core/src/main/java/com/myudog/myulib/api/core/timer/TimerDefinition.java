package com.myudog.myulib.api.core.timer;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.UUID;

/**
 * TimerDefinition
 *
 * 系統：核心計時系統 (Core Timer System)
 * 角色：計時器的藍圖（Blueprint），定義了計時器的長度、行為模式以及各個生命週期階段的事件回呼。
 * 類型：Configuration / Domain Object
 *
 * 此類別採用 Fluent API 設計風格，允許開發者透過串接方法來快速定義計時器的行為，
 * 例如：`new TimerDefinition(...).onStarted(...).onCompleted(...)`。
 */
public final class TimerDefinition {

    /** 藍圖的唯一識別碼。 */
    public final UUID uuid;

    /** 計時器的總持續刻數。 */
    public final long durationTicks;

    /** 計時模式 (正數 COUNT_UP 或 倒數 COUNT_DOWN)。 */
    public final TimerMode mode;

    /** 是否在計時完成後自動將實例轉為停止狀態。 */
    public final boolean autoStopOnComplete;

    /** 流逝時間綁定集合。 */
    public final Map<Integer, TimerBinding> elapsedBindings = new LinkedHashMap<>();

    /** 剩餘時間綁定集合。 */
    public final Map<Integer, TimerBinding> remainingBindings = new LinkedHashMap<>();

    /** 當計時器啟動時執行的動作列表。 */
    public final List<TimerAction> startedActions = new ArrayList<>();

    /** 當計時器暫停時執行的動作列表。 */
    public final List<TimerAction> pausedActions = new ArrayList<>();

    /** 當計時器恢復時執行的動作列表。 */
    public final List<TimerAction> resumedActions = new ArrayList<>();

    /** 當計時器重置時執行的動作列表。 */
    public final List<TimerAction> resetActions = new ArrayList<>();

    /** 當計時器停止時執行的動作列表。 */
    public final List<TimerAction> stoppedActions = new ArrayList<>();

    /** 當計時器完成時執行的動作列表。 */
    public final List<TimerAction> completedActions = new ArrayList<>();

    private int nextBindingId = 1;

    public TimerDefinition(@NotNull UUID uuid, long durationTicks) {
        this(uuid, durationTicks, TimerMode.COUNT_UP, true);
    }

    public TimerDefinition(@NotNull UUID uuid, long durationTicks, TimerMode mode, boolean autoStopOnComplete) {
        this.uuid = Objects.requireNonNull(uuid, "uuid 不得為空");
        this.durationTicks = Math.max(0L, durationTicks);
        this.mode = mode == null ? TimerMode.COUNT_UP : mode;
        this.autoStopOnComplete = autoStopOnComplete;
    }

    // --- 事件綁定 API (Fluent Style) ---

    /**
     * 註冊在指定「已流逝刻數」時觸發的動作。
     */
    public TimerDefinition onElapsedTick(long tick, TimerAction action) { return onElapsedTick(tick, action, false); }

    /**
     * 註冊在指定「剩餘刻數」時觸發的動作。
     */
    public TimerDefinition onRemainingTick(long tick, TimerAction action) { return onRemainingTick(tick, action, false); }

    public TimerDefinition onElapsedTick(long tick, TimerAction action, boolean replace) { return addBinding(elapsedBindings, Set.of(tick), TimerTickBasis.ELAPSED, action, replace); }
    public TimerDefinition onRemainingTick(long tick, TimerAction action, boolean replace) { return addBinding(remainingBindings, Set.of(tick), TimerTickBasis.REMAINING, action, replace); }

    public TimerDefinition onElapsedTick(TimerAction action, int... ticks) { return onElapsedTick(action, false, ticks); }
    public TimerDefinition onRemainingTick(TimerAction action, int... ticks) { return onRemainingTick(action, false, ticks); }

    public TimerDefinition onElapsedTick(TimerAction action, boolean replace, int... ticks) {
        return addBinding(elapsedBindings, normalizeTicks(ticks), TimerTickBasis.ELAPSED, action, replace);
    }
    public TimerDefinition onRemainingTick(TimerAction action, boolean replace, int... ticks) {
        return addBinding(remainingBindings, normalizeTicks(ticks), TimerTickBasis.REMAINING, action, replace);
    }

    // --- 生命週期回呼 API ---

    /** 當計時器啟動時執行的邏輯。 */
    public TimerDefinition onStarted(TimerAction action) { startedActions.add(action); return this; }

    /** 當計時器暫停時執行的邏輯。 */
    public TimerDefinition onPaused(TimerAction action) { pausedActions.add(action); return this; }

    /** 當計時器恢復時執行的邏輯。 */
    public TimerDefinition onResumed(TimerAction action) { resumedActions.add(action); return this; }

    /** 當計時器重置時執行的邏輯。 */
    public TimerDefinition onReset(TimerAction action) { resetActions.add(action); return this; }

    /** 當計時器停止時執行的邏輯。 */
    public TimerDefinition onStopped(TimerAction action) { stoppedActions.add(action); return this; }

    /** 當計時器完成時執行的邏輯。 */
    public TimerDefinition onCompleted(TimerAction action) { completedActions.add(action); return this; }

    public boolean removeBinding(int bindingId) { return elapsedBindings.remove(bindingId) != null || remainingBindings.remove(bindingId) != null; }

    private TimerDefinition addBinding(Map<Integer, TimerBinding> bindings, Set<Long> ticks, TimerTickBasis basis, TimerAction action, boolean replace) {
        if (ticks == null || ticks.isEmpty() || action == null || basis == null) {
            return this;
        }
        int id = nextBindingId++;
        if (!replace && bindings.values().stream().anyMatch(binding -> binding.basis() == basis && !Collections.disjoint(binding.ticks(), ticks))) {
            return this;
        }
        bindings.put(id, new TimerBinding(id, ticks, basis, action));
        return this;
    }

    private static Set<Long> normalizeTicks(int... ticks) {
        if (ticks == null || ticks.length == 0) {
            return Set.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (int tick : ticks) {
            if (tick < 0) {
                continue;
            }
            normalized.add((long) tick);
        }
        return normalized;
    }
}
