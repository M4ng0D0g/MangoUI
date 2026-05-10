package com.myudog.myulib.api.core.camera;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 代表攝影機追蹤的目標。
 * 支援靜態座標與動態實體 ID，可在伺服器與客戶端之間安全傳輸。
 */
public final class CameraTrackingTarget {

    // --- 核心資料 (可安全序列化傳送) ---
    @Nullable private final Vec3 staticPosition;
    @Nullable private final Integer entityId;

    @Nullable private final Vec3 staticLookAt;
    @Nullable private final Integer lookAtEntityId;

    private final Vec3 offset;

    // 全參數私有建構子
    private CameraTrackingTarget(
            @Nullable Vec3 staticPosition,
            @Nullable Integer entityId,
            @Nullable Vec3 staticLookAt,
            @Nullable Integer lookAtEntityId,
            Vec3 offset) {
        this.staticPosition = staticPosition;
        this.entityId = entityId;
        this.staticLookAt = staticLookAt;
        this.lookAtEntityId = lookAtEntityId;
        this.offset = offset == null ? Vec3.ZERO : offset;
    }

    // --- 靜態工廠方法 ---

    /** 從實體物件建立追蹤目標 (常用於伺服器端 API) */
    public static CameraTrackingTarget of(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return new CameraTrackingTarget(null, entity.getId(), null, null, Vec3.ZERO);
    }

    /** 從靜態座標建立追蹤目標 */
    public static CameraTrackingTarget of(Vec3 position) {
        Objects.requireNonNull(position, "position");
        return new CameraTrackingTarget(position, null, null, null, Vec3.ZERO);
    }

    /** 從實體 ID 建立追蹤目標 (供網路封包解析使用) */
    public static CameraTrackingTarget ofEntityId(int id) {
        return new CameraTrackingTarget(null, id, null, null, Vec3.ZERO);
    }

    // --- 鏈式設定 (Builder 模式) ---

    /** 設定攝影機相對於目標的偏移量 */
    public CameraTrackingTarget withOffset(Vec3 offset) {
        return new CameraTrackingTarget(this.staticPosition, this.entityId, this.staticLookAt, this.lookAtEntityId, offset);
    }

    /** 設定攝影機注視特定實體 */
    public CameraTrackingTarget lookAt(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return new CameraTrackingTarget(this.staticPosition, this.entityId, null, entity.getId(), this.offset);
    }

    /** 設定攝影機注視特定靜態座標 */
    public CameraTrackingTarget lookAt(Vec3 point) {
        Objects.requireNonNull(point, "point");
        return new CameraTrackingTarget(this.staticPosition, this.entityId, point, null, this.offset);
    }

    /** 設定攝影機注視特定實體 ID (供網路封包解析使用) */
    public CameraTrackingTarget lookAtEntityId(int id) {
        return new CameraTrackingTarget(this.staticPosition, this.entityId, null, id, this.offset);
    }

    // --- 資料讀取 (供 Payload 打包使用) ---

    @Nullable public Vec3 getStaticPosition() { return staticPosition; }
    @Nullable public Integer getEntityId() { return entityId; }
    @Nullable public Vec3 getStaticLookAt() { return staticLookAt; }
    @Nullable public Integer getLookAtEntityId() { return lookAtEntityId; }
    public Vec3 getOffset() { return offset; }

    // --- 客戶端即時解析邏輯 (Client-Side Rendering) ---

    /**
     * 根據當前世界即時解析出目標的絕對座標。
     * 客戶端應在每一幀呼叫此方法以實現平滑追蹤。
     */
    public Vec3 resolvePosition(Level level) {
        Vec3 basePos = null;

        if (staticPosition != null) {
            basePos = staticPosition;
        } else if (entityId != null && level != null) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                basePos = entity.position();
            }
        }

        if (basePos == null) {
            return offset;
        }
        return basePos.add(offset);
    }

    /**
     * 即時解析出注視點 (LookAt) 座標。
     */
    @Nullable
    public Vec3 resolveLookAt(Level level) {
        if (staticLookAt != null) {
            return staticLookAt;
        } else if (lookAtEntityId != null && level != null) {
            Entity entity = level.getEntity(lookAtEntityId);
            if (entity != null) {
                // 注視實體的中心位置 (重心) 而非腳底
                return entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            }
        }
        return null;
    }
}