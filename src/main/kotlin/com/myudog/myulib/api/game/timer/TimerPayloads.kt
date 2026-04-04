package com.myudog.myulib.api.game.timer

import net.minecraft.util.Identifier
import java.util.UUID

/**
 * Timer 的 payload marker。
 *
 * Timer 本體保留 Any? 是為了支援任意商業資料；
 * 這個 marker 只是提供較好讀的慣例與文件參考。
 */
interface TimerPayload

/**
 * 玩家復活倒數的示例 payload。
 */
data class RespawnTimerPayload(
    val playerUuid: UUID,
    val respawnWorld: Identifier? = null,
    val respawnReason: String? = null,
    val allowSkip: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) : TimerPayload

/**
 * 播放音效的示例 payload。
 */
data class SoundTimerPayload(
    val soundId: Identifier,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f,
    val seed: Long? = null,
    val metadata: Map<String, String> = emptyMap()
) : TimerPayload

/**
 * 安全讀取 payload，適合在事件回呼中使用。
 */
inline fun <reified T> TimerSnapshot.payloadAs(): T? = payload as? T

/**
 * 強制讀取 payload。
 */
inline fun <reified T> TimerSnapshot.requirePayload(): T = payload as T

