package com.myudog.myulib.internal.game.timer

import com.myudog.myulib.api.MyulibApi
import com.myudog.myulib.api.event.ProcessResult
import com.myudog.myulib.api.event.ServerEventBus
import com.myudog.myulib.api.event.events.ServerTickEvent
import com.myudog.myulib.api.game.timer.Timer
import com.myudog.myulib.api.game.timer.TimerCheckpointEvent
import com.myudog.myulib.api.game.timer.TimerCompletedEvent
import com.myudog.myulib.api.game.timer.TimerInstance
import com.myudog.myulib.api.game.timer.TimerPausedEvent
import com.myudog.myulib.api.game.timer.TimerResetEvent
import com.myudog.myulib.api.game.timer.TimerResumedEvent
import com.myudog.myulib.api.game.timer.TimerSnapshot
import com.myudog.myulib.api.game.timer.TimerStartedEvent
import com.myudog.myulib.api.game.timer.TimerStatus
import com.myudog.myulib.api.game.timer.TimerStoppedEvent
import com.myudog.myulib.api.game.timer.TimerTickEvent
import com.myudog.myulib.api.game.timer.toSnapshot
import com.myudog.myulib.api.ecs.EcsWorld
import net.minecraft.util.Identifier

internal class TimerRegistry {
    private val timers = mutableMapOf<Identifier, Timer>()

    fun register(timer: Timer) {
        timers[timer.id] = timer
    }

    fun unregister(timerId: Identifier) {
        timers.remove(timerId)
    }

    fun get(timerId: Identifier): Timer? = timers[timerId]

    fun has(timerId: Identifier): Boolean = timers.containsKey(timerId)
}

internal object DefaultTimerManager {
    private val registry = TimerRegistry()
    private var installed = false
    private var currentTick = 0L

    fun install() {
        if (installed) return
        installed = true
        ServerEventBus.subscribe<ServerTickEvent> { _ ->
            update(MyulibApi.world)
            ProcessResult.PASS
        }
    }

    fun register(timer: Timer) {
        registry.register(timer)
    }

    fun unregister(timerId: Identifier) {
        registry.unregister(timerId)
    }

    fun has(timerId: Identifier): Boolean = registry.has(timerId)

    fun createInstance(
        timerId: Identifier,
        ownerEntityId: Int? = null,
        payload: Any? = null,
        autoStart: Boolean = true,
        world: EcsWorld = MyulibApi.world
    ): Int {
        val definition = registry.get(timerId)
            ?: error("Timer definition '$timerId' is not registered")

        val entityId = world.createEntity()
        val instance = TimerInstance(
            timerId = timerId,
            ownerEntityId = ownerEntityId,
            payload = payload,
            status = if (autoStart) TimerStatus.RUNNING else TimerStatus.IDLE,
            elapsedTicks = 0,
            lastUpdatedTick = currentTick,
            pausedTicks = 0
        )
        world.addComponent(entityId, instance)

        if (autoStart) {
            val snapshot = instance.toSnapshot(entityId, definition, currentTick)
            definition.fire(definition.startedActions, snapshot)
            world.eventBus.dispatch(TimerStartedEvent(snapshot))
        }

        return entityId
    }

    fun getInstance(timerEntityId: Int, world: EcsWorld = MyulibApi.world): TimerInstance? =
        world.getComponent(timerEntityId, TimerInstance::class)

    fun getSnapshot(timerEntityId: Int, world: EcsWorld = MyulibApi.world): TimerSnapshot? {
        val instance = getInstance(timerEntityId, world) ?: return null
        val timer = registry.get(instance.timerId) ?: return null
        return instance.toSnapshot(timerEntityId, timer, currentTick)
    }

    fun findInstances(ownerEntityId: Int, world: EcsWorld = MyulibApi.world): List<Int> {
        return world.query(TimerInstance::class).filter { entityId ->
            world.getComponent(entityId, TimerInstance::class)?.ownerEntityId == ownerEntityId
        }
    }

    fun isRunning(timerEntityId: Int, world: EcsWorld = MyulibApi.world): Boolean =
        getInstance(timerEntityId, world)?.isRunning() == true

    fun isPaused(timerEntityId: Int, world: EcsWorld = MyulibApi.world): Boolean =
        getInstance(timerEntityId, world)?.isPaused() == true

    fun isStopped(timerEntityId: Int, world: EcsWorld = MyulibApi.world): Boolean =
        getInstance(timerEntityId, world)?.isStopped() == true

    fun isCompleted(timerEntityId: Int, world: EcsWorld = MyulibApi.world): Boolean =
        getInstance(timerEntityId, world)?.isCompleted() == true

    fun start(timerEntityId: Int, world: EcsWorld = MyulibApi.world) {
        val instance = getInstance(timerEntityId, world) ?: return
        val timer = registry.get(instance.timerId) ?: return

        if (instance.isCompleted() || instance.isStopped()) {
            instance.elapsedTicks = 0
            instance.pausedTicks = 0
        }

        instance.status = TimerStatus.RUNNING
        instance.lastUpdatedTick = currentTick
        val snapshot = instance.toSnapshot(timerEntityId, timer, currentTick)
        timer.fire(timer.startedActions, snapshot)
        world.eventBus.dispatch(TimerStartedEvent(snapshot))
    }

    fun pause(timerEntityId: Int, world: EcsWorld = MyulibApi.world) {
        val instance = getInstance(timerEntityId, world) ?: return
        val timer = registry.get(instance.timerId) ?: return
        if (instance.status != TimerStatus.RUNNING) return

        instance.status = TimerStatus.PAUSED
        instance.lastUpdatedTick = currentTick
        val snapshot = instance.toSnapshot(timerEntityId, timer, currentTick)
        timer.fire(timer.pausedActions, snapshot)
        world.eventBus.dispatch(TimerPausedEvent(snapshot))
    }

    fun resume(timerEntityId: Int, world: EcsWorld = MyulibApi.world) {
        val instance = getInstance(timerEntityId, world) ?: return
        val timer = registry.get(instance.timerId) ?: return
        if (instance.status != TimerStatus.PAUSED) return

        instance.status = TimerStatus.RUNNING
        instance.lastUpdatedTick = currentTick
        val snapshot = instance.toSnapshot(timerEntityId, timer, currentTick)
        timer.fire(timer.resumedActions, snapshot)
        world.eventBus.dispatch(TimerResumedEvent(snapshot))
    }

    fun stop(timerEntityId: Int, world: EcsWorld = MyulibApi.world) {
        val instance = getInstance(timerEntityId, world) ?: return
        val timer = registry.get(instance.timerId) ?: return

        instance.status = TimerStatus.STOPPED
        instance.lastUpdatedTick = currentTick
        val snapshot = instance.toSnapshot(timerEntityId, timer, currentTick)
        timer.fire(timer.stoppedActions, snapshot)
        world.eventBus.dispatch(TimerStoppedEvent(snapshot))
    }

    fun reset(timerEntityId: Int, clearPayload: Boolean = false, world: EcsWorld = MyulibApi.world) {
        val instance = getInstance(timerEntityId, world) ?: return
        val timer = registry.get(instance.timerId) ?: return

        instance.elapsedTicks = 0
        instance.pausedTicks = 0
        instance.status = TimerStatus.IDLE
        instance.lastUpdatedTick = currentTick
        if (clearPayload) {
            instance.payload = null
        }

        val snapshot = instance.toSnapshot(timerEntityId, timer, currentTick)
        timer.fire(timer.resetActions, snapshot)
        world.eventBus.dispatch(TimerResetEvent(snapshot))
    }

    fun setElapsedTicks(timerEntityId: Int, elapsedTicks: Int, world: EcsWorld = MyulibApi.world) {
        val instance = getInstance(timerEntityId, world) ?: return
        val timer = registry.get(instance.timerId) ?: return
        instance.elapsedTicks = elapsedTicks.coerceAtLeast(0).coerceAtMost(timer.durationTicks)
        instance.lastUpdatedTick = currentTick
    }

    fun setRemainingTicks(timerEntityId: Int, remainingTicks: Int, world: EcsWorld = MyulibApi.world) {
        val instance = getInstance(timerEntityId, world) ?: return
        val timer = registry.get(instance.timerId) ?: return
        val elapsed = (timer.durationTicks - remainingTicks.coerceAtLeast(0)).coerceIn(0, timer.durationTicks)
        instance.elapsedTicks = elapsed
        instance.lastUpdatedTick = currentTick
    }

    fun setPayload(timerEntityId: Int, payload: Any?, world: EcsWorld = MyulibApi.world) {
        val instance = getInstance(timerEntityId, world) ?: return
        instance.payload = payload
    }

    fun update(world: EcsWorld = MyulibApi.world) {
        currentTick += 1
        val timerEntities = world.query(TimerInstance::class)
        for (timerEntityId in timerEntities) {
            val instance = world.getComponent(timerEntityId, TimerInstance::class) ?: continue
            val timer = registry.get(instance.timerId) ?: continue

            when (instance.status) {
                TimerStatus.RUNNING -> {
                    instance.elapsedTicks = (instance.elapsedTicks + 1).coerceAtMost(timer.durationTicks)
                    instance.lastUpdatedTick = currentTick

                    val snapshot = instance.toSnapshot(timerEntityId, timer, currentTick)
                    world.eventBus.dispatch(TimerTickEvent(snapshot))

                    timer.elapsedBindingsAt(snapshot.elapsedTicks).forEach { binding ->
                        binding.action.invoke(snapshot)
                        world.eventBus.dispatch(TimerCheckpointEvent(snapshot, binding.id, binding.basis, binding.tick))
                    }

                    timer.remainingBindingsAt(snapshot.remainingTicks).forEach { binding ->
                        binding.action.invoke(snapshot)
                        world.eventBus.dispatch(TimerCheckpointEvent(snapshot, binding.id, binding.basis, binding.tick))
                    }

                    if (snapshot.elapsedTicks >= timer.durationTicks) {
                        complete(timerEntityId, world, timer, instance)
                    }
                }
                TimerStatus.PAUSED -> {
                    instance.pausedTicks += 1
                    instance.lastUpdatedTick = currentTick
                }
                else -> Unit
            }
        }
    }

    private fun complete(
        timerEntityId: Int,
        world: EcsWorld,
        timer: Timer,
        instance: TimerInstance
    ) {
        if (instance.status == TimerStatus.COMPLETED) return

        instance.status = TimerStatus.COMPLETED
        instance.lastUpdatedTick = currentTick
        val snapshot = instance.toSnapshot(timerEntityId, timer, currentTick)
        timer.fire(timer.completedActions, snapshot)
        world.eventBus.dispatch(TimerCompletedEvent(snapshot))

        if (timer.autoStopOnComplete) {
            instance.status = TimerStatus.COMPLETED
        }
    }
}
