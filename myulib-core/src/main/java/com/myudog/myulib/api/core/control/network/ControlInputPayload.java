package com.myudog.myulib.api.core.control.network;

import com.myudog.myulib.Myulib;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/**
 * 玩家遙控實體的輸入封包資料
 */
public record ControlInputPayload(
        boolean up,
        boolean down,
        boolean left,
        boolean right,
        boolean jumping,
        boolean sneaking,
        float yaw,   // 玩家當前的視角 X 軸旋轉 (滑鼠左右)
        float pitch  // 玩家當前的視角 Y 軸旋轉 (滑鼠上下)
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "control_input");
    public static final Type<ControlInputPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ControlInputPayload> CODEC =
            StreamCodec.of(ControlInputPayload::encode, ControlInputPayload::decode);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static ControlInputPayload decode(RegistryFriendlyByteBuf buf) {
        return new ControlInputPayload(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    private static void encode(RegistryFriendlyByteBuf buf, ControlInputPayload payload) {
        buf.writeBoolean(payload.up);
        buf.writeBoolean(payload.down);
        buf.writeBoolean(payload.left);
        buf.writeBoolean(payload.right);
        buf.writeBoolean(payload.jumping);
        buf.writeBoolean(payload.sneaking);
        buf.writeFloat(payload.yaw);
        buf.writeFloat(payload.pitch);
    }
}