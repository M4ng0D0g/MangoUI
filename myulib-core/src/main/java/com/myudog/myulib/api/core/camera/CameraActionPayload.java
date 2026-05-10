package com.myudog.myulib.api.core.camera;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.core.animation.Easing;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public record CameraActionPayload(
        ActionType action,
        float intensity,
        long durationMillis,
        Vec3 targetStaticPos,
        Integer targetEntityId,
        Vec3 lookAtStaticPos,
        Integer lookAtEntityId,
        Vec3 offset,
        Easing easing
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "camera_action");
    public static final Type<CameraActionPayload> TYPE = new Type<>(ID);

    // 🌟 實作 StreamCodec 來處理 Nullable 欄位
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraActionPayload> CODEC = StreamCodec.of(
            CameraActionPayload::encode, CameraActionPayload::decode
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static CameraActionPayload shake(float intensity, long durationMillis) {
        return new CameraActionPayload(
                ActionType.SHAKE, intensity, durationMillis,
                null, null, null, null, Vec3.ZERO, Easing.SMOOTH_STEP
        );
    }

    public static CameraActionPayload moveTo(CameraTrackingTarget target, long durationMillis, Easing easing) {
        return new CameraActionPayload(
                ActionType.MOVE_TO, 0.0f, durationMillis,
                target.getStaticPosition(), target.getEntityId(),
                target.getStaticLookAt(), target.getLookAtEntityId(),
                target.getOffset(), easing == null ? Easing.SMOOTH_STEP : easing
        );
    }

    public static CameraActionPayload reset() {
        return new CameraActionPayload(
                ActionType.RESET, 0.0f, 0L,
                null, null, null, null, Vec3.ZERO, Easing.LINEAR
        );
    }

    private static void encode(RegistryFriendlyByteBuf buf, CameraActionPayload payload) {
        buf.writeEnum(payload.action);
        buf.writeFloat(payload.intensity);
        buf.writeLong(payload.durationMillis);

        // 處理 Nullable 座標與 ID
        writeNullableVec3(buf, payload.targetStaticPos);
        writeNullableInt(buf, payload.targetEntityId);
        writeNullableVec3(buf, payload.lookAtStaticPos);
        writeNullableInt(buf, payload.lookAtEntityId);

        writeNullableVec3(buf, payload.offset);
        buf.writeEnum(payload.easing != null ? payload.easing : Easing.LINEAR);
    }

    private static CameraActionPayload decode(RegistryFriendlyByteBuf buf) {
        return new CameraActionPayload(
                buf.readEnum(ActionType.class),
                buf.readFloat(),
                buf.readLong(),
                readNullableVec3(buf),
                readNullableInt(buf),
                readNullableVec3(buf),
                readNullableInt(buf),
                readNullableVec3(buf) == null ? Vec3.ZERO : readNullableVec3(buf), // Offset 不可為 null
                buf.readEnum(Easing.class)
        );
    }

    // --- 輔助方法：處理 Nullable 寫入與讀取 ---
    private static void writeNullableVec3(RegistryFriendlyByteBuf buf, Vec3 vec) {
        if (vec != null) {
            buf.writeBoolean(true);
            buf.writeDouble(vec.x); buf.writeDouble(vec.y); buf.writeDouble(vec.z);
        } else {
            buf.writeBoolean(false);
        }
    }
    private static Vec3 readNullableVec3(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
    }
    private static void writeNullableInt(RegistryFriendlyByteBuf buf, Integer val) {
        if (val != null) { buf.writeBoolean(true); buf.writeInt(val); } 
        else { buf.writeBoolean(false); }
    }
    private static Integer readNullableInt(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readInt() : null;
    }

    // ... 保留你原本的 shake(), moveTo(), reset() 工廠方法與 Enum ...
}