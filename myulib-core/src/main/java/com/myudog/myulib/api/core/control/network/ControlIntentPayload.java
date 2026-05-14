package com.myudog.myulib.api.core.control.network;

import com.myudog.myulib.MyulibCore;
import com.myudog.myulib.api.core.control.InputAction;
import com.myudog.myulib.api.core.control.IntentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

/**
 * 控制意圖網路載荷
 * <p>
 * 修正：新增 InputAction 與 timestamp 同步。
 */
public record ControlIntentPayload(
        IntentType intentType,
        InputAction inputAction,
        Vec3 vector,
        int keyCode,
        String customAction,
        long timestamp
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(MyulibCore.MOD_ID, "control_intent");
    public static final Type<ControlIntentPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ControlIntentPayload> CODEC = StreamCodec.of(
            ControlIntentPayload::encode, ControlIntentPayload::decode
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, ControlIntentPayload payload) {
        buf.writeEnum(payload.intentType);
        buf.writeEnum(payload.inputAction);
        buf.writeDouble(payload.vector.x);
        buf.writeDouble(payload.vector.y);
        buf.writeDouble(payload.vector.z);
        buf.writeInt(payload.keyCode);
        buf.writeLong(payload.timestamp);

        // 處理 nullable 的自定義字串
        if (payload.customAction != null) {
            buf.writeBoolean(true);
            buf.writeUtf(payload.customAction);
        } else {
            buf.writeBoolean(false);
        }
    }

    private static ControlIntentPayload decode(RegistryFriendlyByteBuf buf) {
        return new ControlIntentPayload(
                buf.readEnum(IntentType.class),
                buf.readEnum(InputAction.class),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readInt(),
                buf.readBoolean() ? buf.readUtf() : null, // 安全讀取字串
                buf.readLong()
        );
    }
}