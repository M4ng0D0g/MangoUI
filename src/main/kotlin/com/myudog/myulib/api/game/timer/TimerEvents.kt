package com.myudog.myulib.api.game.timer

import com.myudog.myulib.api.event.Event

/**
 * timer 啟動事件。
 */
data class TimerStartedEvent(val snapshot: TimerSnapshot) : Event

/**
 * timer 暫停事件。
 */
data class TimerPausedEvent(val snapshot: TimerSnapshot) : Event

/**
 * timer 恢復事件。
 */
data class TimerResumedEvent(val snapshot: TimerSnapshot) : Event

/**
 * timer 重製事件。
 */
data class TimerResetEvent(val snapshot: TimerSnapshot) : Event

/**
 * timer 停止事件。
 */
data class TimerStoppedEvent(val snapshot: TimerSnapshot) : Event

/**
 * 每次更新都會觸發的 tick 事件。
 */
data class TimerTickEvent(val snapshot: TimerSnapshot) : Event

/**
 * 命中某個特定刻度時觸發的 checkpoint 事件。
 */
data class TimerCheckpointEvent(
    val snapshot: TimerSnapshot,
    val bindingId: java.util.UUID,
    val basis: TimerTickBasis,
    val tick: Int
) : Event

/**
 * timer 完成事件。
 */
data class TimerCompletedEvent(val snapshot: TimerSnapshot) : Event

