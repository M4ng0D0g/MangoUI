package com.myudog.myulib.api.game.timer

import com.myudog.myulib.api.MyulibApi
import com.myudog.myulib.api.ecs.EcsWorld
import com.myudog.myulib.internal.game.timer.DefaultTimerManager
import net.minecraft.util.Identifier

/**
 * Timer 系統對外入口。
 *
 * 這層只負責轉發到 internal manager，保留乾淨的 API 邊界。
 */
object TimerManager {
    fun install() {
        DefaultTimerManager.install()
    }

    fun register(timer: Timer) {
        DefaultTimerManager.register(timer)
    }

    fun unregister(timerId: Identifier) {
        DefaultTimerManager.unregister(timerId)
    }

    fun has(timerId: Identifier): Boolean = DefaultTimerManager.has(timerId)

    fun createInstance(
        timerId: Identifier,
        ownerEntityId: Int? = null,
        payload: Any? = null,
        autoStart: Boolean = true,
        world: EcsWorld = MyulibApi.world
    ): Int = DefaultTimerManager.createInstance(timerId, ownerEntityId, payload, autoStart, world)

    fun getInstance(timerEntityId: Int, world: EcsWorld = MyulibApi.world): TimerInstance? =
        DefaultTimerManager.getInstance(timerEntityId, world)

    fun getSnapshot(timerEntityId: Int, world: EcsWorld = MyulibApi.world): TimerSnapshot? =
        DefaultTimerManager.getSnapshot(timerEntityId, world)

    fun findInstances(ownerEntityId: Int, world: EcsWorld = MyulibApi.world): List<Int> =
        DefaultTimerManager.findInstances(ownerEntityId, world)

    fun isRunning(timerEntityId: Int, world: EcsWorld = MyulibApi.world): Boolean =
        DefaultTimerManager.isRunning(timerEntityId, world)

    fun isPaused(timerEntityId: Int, world: EcsWorld = MyulibApi.world): Boolean =
        DefaultTimerManager.isPaused(timerEntityId, world)

    fun isStopped(timerEntityId: Int, world: EcsWorld = MyulibApi.world): Boolean =
        DefaultTimerManager.isStopped(timerEntityId, world)

    fun isCompleted(timerEntityId: Int, world: EcsWorld = MyulibApi.world): Boolean =
        DefaultTimerManager.isCompleted(timerEntityId, world)

    fun start(timerEntityId: Int, world: EcsWorld = MyulibApi.world) {
        DefaultTimerManager.start(timerEntityId, world)
    }

    fun pause(timerEntityId: Int, world: EcsWorld = MyulibApi.world) {
        DefaultTimerManager.pause(timerEntityId, world)
    }

    fun resume(timerEntityId: Int, world: EcsWorld = MyulibApi.world) {
        DefaultTimerManager.resume(timerEntityId, world)
    }

    fun stop(timerEntityId: Int, world: EcsWorld = MyulibApi.world) {
        DefaultTimerManager.stop(timerEntityId, world)
    }

    fun reset(timerEntityId: Int, clearPayload: Boolean = false, world: EcsWorld = MyulibApi.world) {
        DefaultTimerManager.reset(timerEntityId, clearPayload, world)
    }

    fun setElapsedTicks(timerEntityId: Int, elapsedTicks: Int, world: EcsWorld = MyulibApi.world) {
        DefaultTimerManager.setElapsedTicks(timerEntityId, elapsedTicks, world)
    }

    fun setRemainingTicks(timerEntityId: Int, remainingTicks: Int, world: EcsWorld = MyulibApi.world) {
        DefaultTimerManager.setRemainingTicks(timerEntityId, remainingTicks, world)
    }

    fun setPayload(timerEntityId: Int, payload: Any?, world: EcsWorld = MyulibApi.world) {
        DefaultTimerManager.setPayload(timerEntityId, payload, world)
    }

    fun update(world: EcsWorld = MyulibApi.world) {
        DefaultTimerManager.update(world)
    }
}

