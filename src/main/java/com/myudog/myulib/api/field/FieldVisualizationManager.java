package com.myudog.myulib.api.field;

import com.myudog.myulib.api.hologram.*;
import com.myudog.myulib.api.hologram.network.HologramNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FieldVisualizationManager {

    public static final FieldVisualizationManager INSTANCE = new FieldVisualizationManager();

    
    private final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Integer> LAST_SYNC_TICK = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> PLAYER_RADIUS = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, HologramStyle> PLAYER_STYLE = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, DisplayMode> PLAYER_MODE = new ConcurrentHashMap<>();
    private volatile boolean installed;
    private final int SYNC_INTERVAL_TICKS = 5;

    public enum DisplayMode {
        EDGES_ONLY,
        FULL,
        LABELS_ONLY;

        public String token() {
            return switch (this) {
                case EDGES_ONLY -> "edges-only";
                case FULL -> "full";
                case LABELS_ONLY -> "labels-only";
            };
        }

        public DisplayMode parse(String raw) {
            String token = raw == null ? "" : raw.trim().toLowerCase().replace('_', '-');
            return switch (token) {
                case "edges", "edges-only" -> EDGES_ONLY;
                case "full" -> FULL;
                case "labels", "labels-only" -> LABELS_ONLY;
                default -> throw new IllegalArgumentException("Unknown display mode: " + raw);
            };
        }
    }

    private FieldVisualizationManager() {}

    public void install() {
        if (installed) return;
        installed = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ENABLED.isEmpty()) return;
            int tick = (int) (server.getTickCount() & Integer.MAX_VALUE);
            for (UUID playerId : Set.copyOf(ENABLED)) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null || !player.isAlive()) {
                    disable(playerId);
                    continue;
                }
                if (player.level() instanceof ServerLevel level) {
                    renderForPlayer(level, player, tick);
                }
            }
        });
    }

    public void enable(UUID playerId) {
        ENABLED.add(playerId);
        PLAYER_RADIUS.putIfAbsent(playerId, 64);
        PLAYER_STYLE.putIfAbsent(playerId, HologramStyle.defaults());
        PLAYER_MODE.putIfAbsent(playerId, DisplayMode.EDGES_ONLY);
    }

    public void disable(UUID playerId) {
        ENABLED.remove(playerId);
        LAST_SYNC_TICK.remove(playerId);
        PLAYER_RADIUS.remove(playerId);
        PLAYER_STYLE.remove(playerId);
        PLAYER_MODE.remove(playerId);
    }

    public void setMode(UUID playerId, DisplayMode mode) {
        PLAYER_MODE.put(playerId, mode);
        switch (mode) {
            case EDGES_ONLY -> PLAYER_STYLE.put(playerId, HologramStyle.defaults().withFeature(HologramFeature.AXES, false));
            case FULL -> PLAYER_STYLE.put(playerId, HologramStyle.full());
            case LABELS_ONLY -> PLAYER_STYLE.put(playerId, HologramStyle.labelsOnly());
        }
    }

    public DisplayMode getMode(UUID playerId) {
        return PLAYER_MODE.getOrDefault(playerId, DisplayMode.EDGES_ONLY);
    }

    public boolean isEnabled(UUID playerId) {
        return ENABLED.contains(playerId);
    }

    public int getRadius(UUID playerId) {
        return PLAYER_RADIUS.getOrDefault(playerId, 64);
    }

    public void setRadius(UUID playerId, int radius) {
        PLAYER_RADIUS.put(playerId, radius);
    }

    public HologramStyle getStyle(UUID playerId) {
        HologramStyle style = PLAYER_STYLE.get(playerId);
        return style == null ? HologramStyle.defaults() : style;
    }

    public void setFeature(UUID playerId, HologramFeature feature, boolean enabled) {
        HologramStyle current = getStyle(playerId);
        PLAYER_STYLE.put(playerId, current.withFeature(feature, enabled));
    }

    private void renderForPlayer(ServerLevel level, ServerPlayer player, int tick) {
        Integer last = LAST_SYNC_TICK.getOrDefault(player.getUUID(), 0);
        if (tick - last < SYNC_INTERVAL_TICKS) return;
        LAST_SYNC_TICK.put(player.getUUID(), tick);

        Vec3 viewer = player.position();
        int radius = PLAYER_RADIUS.getOrDefault(player.getUUID(), 64);
        HologramStyle style = getStyle(player.getUUID());
        List<HologramDefinition> visible = new ArrayList<>();

        // 1. 同步區域 (Field) 資料
        for (FieldDefinition field : FieldManager.INSTANCE.all().values()) {
            if (!field.dimensionId().equals(level.dimension().identifier())) continue;
            if (distanceToAabb(viewer, field.bounds()) > radius) continue;

            visible.add(new HologramDefinition(
                    field.id(),
                    field.dimensionId(),
                    field.bounds(),
                    field.id().getPath(),
                    style
            ));
        }

        // 2. 同步獨立的全息投影資料
        for (HologramDefinition holo : HologramManager.INSTANCE.all().values()) {
            if (!holo.dimensionId().equals(level.dimension().identifier())) continue;
            if (distanceToAabb(viewer, holo.bounds()) > radius) continue;
            visible.add(holo);
        }

        HologramNetworking.syncToPlayer(player, visible);
    }

    private double distanceToAabb(Vec3 pos, AABB box) {
        double dx = Math.max(Math.max(box.minX - pos.x, 0.0), pos.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - pos.y, 0.0), pos.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - pos.z, 0.0), pos.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}