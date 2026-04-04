package com.myudog.myulib.api.game.timer

import net.minecraft.util.Identifier

/**
 * 可直接參考的玩家復活 timer 範例。
 *
 * 注意：這個類別刻意只示範 timer 與 payload 的用法，
 * 真正的復活判定/傳送/狀態同步應由商業邏輯層自行接手。
 */
object RespawnTimerExample {
    fun registerRespawnTimer(
        timerId: Identifier,
        durationTicks: Int = 20 * 5,
        onWarning: (TimerSnapshot, RespawnTimerPayload) -> Unit = { _, _ -> },
        onRespawn: (TimerSnapshot, RespawnTimerPayload) -> Unit = { _, _ -> }
    ): Timer {
        val timer = Timer(
            id = timerId,
            durationTicks = durationTicks,
            mode = TimerMode.COUNT_DOWN,
            autoStopOnComplete = true
        )
            .onRemainingTick(60) { snapshot ->
                val payload = snapshot.payloadAs<RespawnTimerPayload>() ?: return@onRemainingTick
                onWarning(snapshot, payload)
            }
            .onRemainingTick(20) { snapshot ->
                val payload = snapshot.payloadAs<RespawnTimerPayload>() ?: return@onRemainingTick
                onWarning(snapshot, payload)
            }
            .onCompleted { snapshot ->
                val payload = snapshot.payloadAs<RespawnTimerPayload>() ?: return@onCompleted
                onRespawn(snapshot, payload)
            }

        TimerManager.register(timer)
        return timer
    }

    fun spawnRespawnTimer(
        timerId: Identifier,
        ownerEntityId: Int,
        payload: RespawnTimerPayload,
        world: com.myudog.myulib.api.ecs.EcsWorld = com.myudog.myulib.api.MyulibApi.world
    ): Int {
        return TimerManager.createInstance(
            timerId = timerId,
            ownerEntityId = ownerEntityId,
            payload = payload,
            autoStart = true,
            world = world
        )
    }

    fun registerSoundCueTimer(
        timerId: Identifier,
        durationTicks: Int = 20 * 3,
        onCue: (TimerSnapshot, SoundTimerPayload) -> Unit = { _, _ -> }
    ): Timer {
        val timer = Timer(
            id = timerId,
            durationTicks = durationTicks,
            mode = TimerMode.COUNT_DOWN,
            autoStopOnComplete = true
        )
            .onRemainingTick(40) { snapshot ->
                val payload = snapshot.payloadAs<SoundTimerPayload>() ?: return@onRemainingTick
                onCue(snapshot, payload)
            }
            .onRemainingTick(20) { snapshot ->
                val payload = snapshot.payloadAs<SoundTimerPayload>() ?: return@onRemainingTick
                onCue(snapshot, payload)
            }
            .onCompleted { snapshot ->
                val payload = snapshot.payloadAs<SoundTimerPayload>() ?: return@onCompleted
                onCue(snapshot, payload)
            }

        TimerManager.register(timer)
        return timer
    }
}

