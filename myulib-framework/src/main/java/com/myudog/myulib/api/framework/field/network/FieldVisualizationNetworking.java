package com.myudog.myulib.api.framework.field.network;

import com.myudog.myulib.Myulib;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class FieldVisualizationNetworking {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "field_visualization_sync");

    public record FieldVisualizationPayload(List<AABB> boxes) implements CustomPacketPayload {
        public static final Type<FieldVisualizationPayload> TYPE = new Type<>(CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, FieldVisualizationPayload> CODEC =
                StreamCodec.of(FieldVisualizationPayload::encode, FieldVisualizationPayload::decode);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static FieldVisualizationPayload decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<AABB> boxes = new ArrayList<>(Math.max(0, size));
            for (int i = 0; i < size; i++) {
                boxes.add(new AABB(
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble()
                ));
            }
            return new FieldVisualizationPayload(List.copyOf(boxes));
        }

        private static void encode(RegistryFriendlyByteBuf buf, FieldVisualizationPayload payload) {
            List<AABB> boxes = payload.boxes == null ? List.of() : payload.boxes;
            buf.writeVarInt(boxes.size());
            for (AABB box : boxes) {
                buf.writeDouble(box.minX);
                buf.writeDouble(box.minY);
                buf.writeDouble(box.minZ);
                buf.writeDouble(box.maxX);
                buf.writeDouble(box.maxY);
                buf.writeDouble(box.maxZ);
            }
        }
    }

    private FieldVisualizationNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(FieldVisualizationPayload.TYPE, FieldVisualizationPayload.CODEC);
    }

    public static void syncToPlayer(ServerPlayer player, List<AABB> boxes) {
        if (player == null) {
            return;
        }
        ServerPlayNetworking.send(player, new FieldVisualizationPayload(boxes == null ? List.of() : List.copyOf(boxes)));
    }
}


