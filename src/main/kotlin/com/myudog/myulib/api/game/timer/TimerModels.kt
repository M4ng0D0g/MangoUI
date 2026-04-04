package com.myudog.myulib.api.game.timer

import com.myudog.myulib.api.ecs.Component
import net.minecraft.util.Identifier
import java.util.UUID

/**
 * 計時器目前的運作模式。
 */
enum class TimerMode {
    COUNT_UP,
    COUNT_DOWN
}

/**
 * 計時器目前的生命週期狀態。
 */
enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED
}

/**
 * 刻度事件的比較基準。
 * ELAPSED = 已經經過幾 tick
 * REMAINING = 還剩幾 tick
 */
enum class TimerTickBasis {
    ELAPSED,
    REMAINING
}

/**
 * 事件回呼：事件會收到完整的快照，可讀取 timer 狀態與 payload。
 */
fun interface TimerAction {
    fun invoke(snapshot: TimerSnapshot)
}

/**
 * 一個刻度事件綁定，對應某個 tick 與 callback。
 */
data class TimerBinding(
    val id: UUID = UUID.randomUUID(),
    val tick: Int,
    val basis: TimerTickBasis,
    val action: TimerAction
)

/**
 * 計時器執行中的不變快照。
 */
data class TimerSnapshot(
    val timerEntityId: Int,
    val ownerEntityId: Int?,
    val timer: Timer,
    val status: TimerStatus,
    val elapsedTicks: Int,
    val remainingTicks: Int,
    val payload: Any?,
    val currentTick: Long
) {
    val durationTicks: Int get() = timer.durationTicks
    val mode: TimerMode get() = timer.mode
    val progress: Float get() = if (durationTicks <= 0) 1.0f else elapsedTicks.coerceAtLeast(0).coerceAtMost(durationTicks).toFloat() / durationTicks.toFloat()
}

/**
 * 享元本體：共享的 timer 定義、刻度事件與生命週期回呼。
 *
 * 這個類別盡量維持可共享、可複用；真正可變的 runtime 狀態由 TimerInstance 承擔。
 */
data class Timer(
    val id: Identifier,
    val durationTicks: Int,
    val mode: TimerMode = TimerMode.COUNT_DOWN,
    val autoStopOnComplete: Boolean = true,
    val elapsedBindings: Map<Int, List<TimerBinding>> = emptyMap(),
    val remainingBindings: Map<Int, List<TimerBinding>> = emptyMap(),
    val startedActions: List<TimerAction> = emptyList(),
    val pausedActions: List<TimerAction> = emptyList(),
    val resumedActions: List<TimerAction> = emptyList(),
    val resetActions: List<TimerAction> = emptyList(),
    val stoppedActions: List<TimerAction> = emptyList(),
    val completedActions: List<TimerAction> = emptyList()
) {
    init {
        require(durationTicks >= 0) { "durationTicks must be >= 0" }
    }

    fun onElapsedTick(tick: Int, action: TimerAction): Timer =
        copy(elapsedBindings = elapsedBindings.plusBinding(TimerBinding(tick = tick, basis = TimerTickBasis.ELAPSED, action = action)))

    fun onRemainingTick(tick: Int, action: TimerAction): Timer =
        copy(remainingBindings = remainingBindings.plusBinding(TimerBinding(tick = tick, basis = TimerTickBasis.REMAINING, action = action)))

    fun onStarted(action: TimerAction): Timer = copy(startedActions = startedActions + action)
    fun onPaused(action: TimerAction): Timer = copy(pausedActions = pausedActions + action)
    fun onResumed(action: TimerAction): Timer = copy(resumedActions = resumedActions + action)
    fun onReset(action: TimerAction): Timer = copy(resetActions = resetActions + action)
    fun onStopped(action: TimerAction): Timer = copy(stoppedActions = stoppedActions + action)
    fun onCompleted(action: TimerAction): Timer = copy(completedActions = completedActions + action)

    fun removeBinding(bindingId: UUID): Timer = copy(
        elapsedBindings = elapsedBindings.withoutBinding(bindingId),
        remainingBindings = remainingBindings.withoutBinding(bindingId)
    )

    internal fun elapsedBindingsAt(tick: Int): List<TimerBinding> = elapsedBindings[tick].orEmpty()
    internal fun remainingBindingsAt(tick: Int): List<TimerBinding> = remainingBindings[tick].orEmpty()

    internal fun fire(actions: List<TimerAction>, snapshot: TimerSnapshot) {
        actions.forEach { it.invoke(snapshot) }
    }

    private fun Map<Int, List<TimerBinding>>.plusBinding(binding: TimerBinding): Map<Int, List<TimerBinding>> {
        val updated = this[binding.tick].orEmpty().toMutableList()
        updated.add(binding)
        return this + (binding.tick to updated.toList())
    }

    private fun Map<Int, List<TimerBinding>>.withoutBinding(bindingId: UUID): Map<Int, List<TimerBinding>> {
        if (isEmpty()) return this
        val updated = toMutableMap()
        val keysToRemove = mutableListOf<Int>()
        for ((tick, bindings) in updated) {
            val filtered = bindings.filterNot { it.id == bindingId }
            if (filtered.isEmpty()) {
                keysToRemove.add(tick)
            } else {
                updated[tick] = filtered
            }
        }
        keysToRemove.forEach { updated.remove(it) }
        return updated.toMap()
    }
}

/**
 * 執行中的 timer 實例：可變狀態只存在這裡。
 *
 * 這個類別同時也是 ECS component，因此可以被 EcsWorld 直接存放與查詢。
 */
data class TimerInstance(
    val timerId: Identifier,
    var ownerEntityId: Int? = null,
    var payload: Any? = null,
    var status: TimerStatus = TimerStatus.IDLE,
    var elapsedTicks: Int = 0,
    var lastUpdatedTick: Long = -1L,
    var pausedTicks: Int = 0
) : Component {
    fun isRunning(): Boolean = status == TimerStatus.RUNNING
    fun isPaused(): Boolean = status == TimerStatus.PAUSED
    fun isStopped(): Boolean = status == TimerStatus.STOPPED
    fun isCompleted(): Boolean = status == TimerStatus.COMPLETED
}

internal fun TimerInstance.toSnapshot(timerEntityId: Int, timer: Timer, currentTick: Long): TimerSnapshot {
    val elapsed = elapsedTicks.coerceAtLeast(0)
    val remaining = when (timer.mode) {
        TimerMode.COUNT_UP -> (timer.durationTicks - elapsed).coerceAtLeast(0)
        TimerMode.COUNT_DOWN -> (timer.durationTicks - elapsed).coerceAtLeast(0)
    }
    return TimerSnapshot(
        timerEntityId = timerEntityId,
        ownerEntityId = ownerEntityId,
        timer = timer,
        status = status,
        elapsedTicks = elapsed,
        remainingTicks = remaining,
        payload = payload,
        currentTick = currentTick
    )
}

