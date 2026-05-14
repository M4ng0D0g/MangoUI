package com.myudog.myulib.api.core.timer;

import com.myudog.myulib.api.core.ecs.IComponent;
import java.util.UUID;

/**
 * TimerInstance
 *
 * 系統：核心計時系統 (Core Timer System)
 * 角色：表示一個正在運行或已存在的計時器實例。
 * 類型：Data Holder / State Object / Component
 *
 * 此類別持有計時器的運行時狀態，包括流逝時間、目前狀態以及關聯的藍圖 ID。
 */
public final class TimerInstance implements IComponent {

    /** 實例的唯一識別碼，用於在 TimerManager 中檢索此實例。 */
    public final UUID instanceId;

    /** 關聯的計時器藍圖 (TimerDefinition) ID。 */
    public final UUID defId;

    /** 持有此計時器的實體 ID (可選)。 */
    public final Long ownerEntityId;

    /** 附加的資料載荷。 */
    public TimerPayload payload;

    /** 目前計時器的狀態 (運行中、暫停等)。 */
    public TimerStatus status;

    /** 已流逝的刻數 (Ticks)。 */
    public long elapsedTicks;

    /** 最後更新的時間戳。 */
    public long lastUpdatedTick;

    /** 已暫停的累計刻數。 */
    public long pausedTicks;

    public TimerInstance(UUID instanceId, UUID defId, Long ownerEntityId, TimerPayload payload) {
        this.instanceId = instanceId;
        this.defId = defId;
        this.ownerEntityId = ownerEntityId;
        this.payload = payload;
        this.status = TimerStatus.IDLE;
    }

    public boolean isRunning() { return status == TimerStatus.RUNNING; }
    public boolean isPaused() { return status == TimerStatus.PAUSED; }
    public boolean isStopped() { return status == TimerStatus.STOPPED; }
    public boolean isCompleted() { return status == TimerStatus.COMPLETED; }
}