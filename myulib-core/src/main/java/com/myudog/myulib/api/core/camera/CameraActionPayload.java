package com.myudog.myulib.api.core.camera;

import com.myudog.myulib.MyulibCore;
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
        Easing easing,
        // --- 新增欄位支援 /camera 指令 ---
        int r, int g, int b,
        float fadeIn, float hold, float fadeOut,
        float fov,
        String stringData
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(MyulibCore.MOD_ID, "camera_action");
    public static final Type<CameraActionPayload> TYPE = new Type<>(ID);

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
                null, null, null, null, Vec3.ZERO, Easing.SMOOTH_STEP,
                0, 0, 0, 0, 0, 0, 0, ""
        );
    }

    public static CameraActionPayload moveTo(CameraTrackingTarget target, long durationMillis, Easing easing) {
        return new CameraActionPayload(
                ActionType.MOVE_TO, 0.0f, durationMillis,
                target.getStaticPosition(), target.getEntityId(),
                target.getStaticLookAt(), target.getLookAtEntityId(),
                target.getOffset(), easing == null ? Easing.SMOOTH_STEP : easing,
                0, 0, 0, 0, 0, 0, 0, ""
        );
    }

    public static CameraActionPayload fade(int r, int g, int b, float in, float hold, float out) {
        return new CameraActionPayload(
                ActionType.FADE, 0, 0,
                null, null, null, null, Vec3.ZERO, Easing.LINEAR,
                r, g, b, in, hold, out, 0, ""
        );
    }

    public static CameraActionPayload setFov(float fov, float easeTime, Easing easing) {
        return new CameraActionPayload(
                ActionType.FOV_SET, 0, (long)(easeTime * 1000),
                null, null, null, null, Vec3.ZERO, easing,
                0, 0, 0, 0, 0, 0, fov, ""
        );
    }

    public static CameraActionPayload clearFov(float easeTime, Easing easing) {
        return new CameraActionPayload(
                ActionType.FOV_CLEAR, 0, (long)(easeTime * 1000),
                null, null, null, null, Vec3.ZERO, easing,
                0, 0, 0, 0, 0, 0, 0, ""
        );
    }

    public static CameraActionPayload setPreset(String preset, CameraTrackingTarget target, long durationMillis, Easing easing) {
        return new CameraActionPayload(
                ActionType.SET_PRESET, 0, durationMillis,
                target.getStaticPosition(), target.getEntityId(),
                target.getStaticLookAt(), target.getLookAtEntityId(),
                target.getOffset(), easing,
                0, 0, 0, 0, 0, 0, 0, preset
        );
    }

    public static CameraActionPayload reset() {
        return new CameraActionPayload(
                ActionType.RESET, 0.0f, 0L,
                null, null, null, null, Vec3.ZERO, Easing.LINEAR,
                0, 0, 0, 0, 0, 0, 0, ""
        );
    }

    private static void encode(RegistryFriendlyByteBuf buf, CameraActionPayload payload) {
        buf.writeEnum(payload.action);
        buf.writeFloat(payload.intensity);
        buf.writeLong(payload.durationMillis);

        writeNullableVec3(buf, payload.targetStaticPos);
        writeNullableInt(buf, payload.targetEntityId);
        writeNullableVec3(buf, payload.lookAtStaticPos);
        writeNullableInt(buf, payload.lookAtEntityId);
        writeNullableVec3(buf, payload.offset);
        buf.writeEnum(payload.easing != null ? payload.easing : Easing.LINEAR);

        buf.writeInt(payload.r);
        buf.writeInt(payload.g);
        buf.writeInt(payload.b);
        buf.writeFloat(payload.fadeIn);
        buf.writeFloat(payload.hold);
        buf.writeFloat(payload.fadeOut);
        buf.writeFloat(payload.fov);
        buf.writeUtf(payload.stringData);
    }

    private static CameraActionPayload decode(RegistryFriendlyByteBuf buf) {
        ActionType action = buf.readEnum(ActionType.class);
        float intensity = buf.readFloat();
        long durationMillis = buf.readLong();
        Vec3 targetStaticPos = readNullableVec3(buf);
        Integer targetEntityId = readNullableInt(buf);
        Vec3 lookAtStaticPos = readNullableVec3(buf);
        Integer lookAtEntityId = readNullableInt(buf);
        Vec3 offset = readNullableVec3(buf);
        if (offset == null) offset = Vec3.ZERO;
        Easing easing = buf.readEnum(Easing.class);

        int r = buf.readInt();
        int g = buf.readInt();
        int b = buf.readInt();
        float fadeIn = buf.readFloat();
        float hold = buf.readFloat();
        float fadeOut = buf.readFloat();
        float fov = buf.readFloat();
        String stringData = buf.readUtf();

        return new CameraActionPayload(
                action, intensity, durationMillis,
                targetStaticPos, targetEntityId,
                lookAtStaticPos, lookAtEntityId,
                offset, easing,
                r, g, b, fadeIn, hold, fadeOut, fov, stringData
        );
    }

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
}