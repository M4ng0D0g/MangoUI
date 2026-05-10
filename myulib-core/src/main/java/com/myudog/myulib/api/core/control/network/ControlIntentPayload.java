package com.myudog.myulib.api.core.control.network;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.core.control.IntentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public record ControlIntentPayload(IntentType intentType, Vec3 vector, int keyCode, String customAction) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "control_intent");
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
        buf.writeDouble(payload.vector.x);
        buf.writeDouble(payload.vector.y);
        buf.writeDouble(payload.vector.z);
        buf.writeInt(payload.keyCode);

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
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readInt(),
                buf.readBoolean() ? buf.readUtf() : null // 安全讀取字串
        );
    }
}