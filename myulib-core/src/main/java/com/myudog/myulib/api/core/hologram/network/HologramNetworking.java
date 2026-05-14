package com.myudog.myulib.api.core.hologram.network;

import com.myudog.myulib.MyulibCore;
import com.myudog.myulib.api.core.hologram.HologramDefinition;
import com.myudog.myulib.api.core.hologram.HologramStyle;
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

public final class HologramNetworking {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MyulibCore.MOD_ID, "hologram_sync");

    public record HologramPayload(List<HologramDefinition> entries) implements CustomPacketPayload {
        public static final Type<HologramPayload> TYPE = new Type<>(CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, HologramPayload> CODEC =
                StreamCodec.of(HologramPayload::encode, HologramPayload::decode);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        private static HologramPayload decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<HologramDefinition> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Identifier id = Identifier.parse(buf.readUtf());
                Identifier dim = Identifier.parse(buf.readUtf());
                AABB bounds = new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble(), buf.readDouble());
                String label = buf.readUtf();
                int color = buf.readInt();
                byte flags = buf.readByte();
                list.add(new HologramDefinition(id, dim, bounds, label, new HologramStyle(color, flags)));
            }
            return new HologramPayload(list);
        }

        private static void encode(RegistryFriendlyByteBuf buf, HologramPayload payload) {
            buf.writeVarInt(payload.entries.size());
            for (HologramDefinition def : payload.entries) {
                buf.writeUtf(def.id().toString());
                buf.writeUtf(def.dimensionId().toString());
                buf.writeDouble(def.bounds().minX); buf.writeDouble(def.bounds().minY); buf.writeDouble(def.bounds().minZ);
                buf.writeDouble(def.bounds().maxX); buf.writeDouble(def.bounds().maxY); buf.writeDouble(def.bounds().maxZ);
                buf.writeUtf(def.label() == null ? "" : def.label());
                buf.writeInt(def.style().color());
                buf.writeByte(def.style().flags());
            }
        }
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(HologramPayload.TYPE, HologramPayload.CODEC);
    }

    public static void syncToPlayer(ServerPlayer player, List<HologramDefinition> holograms) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new HologramPayload(holograms));
    }
}