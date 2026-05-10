package com.myudog.myulib.api.core.control.network;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.core.control.ControlManager;
import com.myudog.myulib.api.core.control.Intent;
import com.myudog.myulib.api.core.control.IPlayerController;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

public final class ServerControlNetworking {
    public static final Identifier CONTROL_STATE_CHANNEL = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "control_state");

    private static boolean payloadsRegistered;
    private static boolean receiversRegistered;

    private ServerControlNetworking() {
    }

    public record ControlStatePayload(int disabledMask, boolean controlling, boolean controlled) implements CustomPacketPayload {
        public static final Type<ControlStatePayload> TYPE = new Type<>(CONTROL_STATE_CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, ControlStatePayload> CODEC =
                StreamCodec.of(ControlStatePayload::encode, ControlStatePayload::decode);

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static ControlStatePayload decode(RegistryFriendlyByteBuf buf) {
            return new ControlStatePayload(buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
        }

        private static void encode(RegistryFriendlyByteBuf buf, ControlStatePayload payload) {
            buf.writeVarInt(payload.disabledMask);
            buf.writeBoolean(payload.controlling);
            buf.writeBoolean(payload.controlled);
        }
    }

    public static synchronized void registerPayloads() {
        if (payloadsRegistered) {
            return;
        }
        payloadsRegistered = true;

        PayloadTypeRegistry.serverboundPlay().register(ControlInputPayload.TYPE, ControlInputPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ControlIntentPayload.TYPE, ControlIntentPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ControlStatePayload.TYPE, ControlStatePayload.CODEC);
    }

    public static synchronized void registerServerReceivers() {
        if (receiversRegistered) {
            return;
        }
        receiversRegistered = true;

        // 修正 1：傳遞 ServerLevel 給 updateInput
        ServerPlayNetworking.registerGlobalReceiver(ControlInputPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player().level() instanceof ServerLevel serverLevel) {
                        ControlManager.INSTANCE.updateInput(context.player(), serverLevel, payload);
                    }
                }));

        // 修正 2：使用新的 Intent 建構子與 dispatch 邏輯
        ServerPlayNetworking.registerGlobalReceiver(ControlIntentPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player() instanceof IPlayerController controller) {
                        Intent intent = new Intent(payload.intentType(), payload.vector(), payload.keyCode(), payload.customAction());
                        controller.myulib_mc$dispatchIntent(intent);
                    }
                }));
    }

    public static void syncControlState(ServerPlayer player, int disabledMask, boolean controlling, boolean controlled) {
        if (player == null) {
            return;
        }
        ServerPlayNetworking.send(player, new ControlStatePayload(disabledMask, controlling, controlled));
    }
}