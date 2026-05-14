package com.myudog.myulib.api.framework.field;

import com.myudog.myulib.api.core.hologram.*;
import com.myudog.myulib.api.core.hologram.network.HologramNetworking;
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

/**
 * FieldVisualizationManager
 *
 * 蝟餌絞嚗??脣?啁頂蝯?(Framework - Field)
 * 閫嚗?啗?閬箏?蝞∠??剁?鞎痊撠撩?蝡舐??游???郊?箏恥?嗥垢??舀?敶?(Hologram)?? * 憿?嚗anager / Renderer (Server-side sidecar)
 *
 * 甇斤頂蝯望????鈭?閬箏?璅∪????拙振嚗蒂?寞??嗡?蝵柴?撠?敺??末?＊蝷箸見撘?(HologramStyle)嚗? * 摰?撠?餈??游???郊蝯血恥?嗥垢?脰?皜脫??? */
public final class FieldVisualizationManager {

    public static final FieldVisualizationManager INSTANCE = new FieldVisualizationManager();

    /** 撌脣??刻?閬箏?璅∪??摰?ID ????*/
    private final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    /** 閮?瘥摰嗆?敺?甈∪?甇亦????喋?*/
    private final ConcurrentHashMap<UUID, Integer> LAST_SYNC_TICK = new ConcurrentHashMap<>();

    /** ?拙振??閬箏???嚗?閮?64 ?憛???*/
    private final ConcurrentHashMap<UUID, Integer> PLAYER_RADIUS = new ConcurrentHashMap<>();

    /** 瘥摰嗅閮剖???舀?敶望見撘?*/
    private final ConcurrentHashMap<UUID, HologramStyle> PLAYER_STYLE = new ConcurrentHashMap<>();

    /** 瘥摰嗅閮剖??＊蝷箸芋撘?(憒??楠???氬?璅惜)??*/
    private final ConcurrentHashMap<UUID, DisplayMode> PLAYER_MODE = new ConcurrentHashMap<>();

    private volatile boolean installed;

    /** 鞈??郊?餌? (Ticks)??*/
    private final int SYNC_INTERVAL_TICKS = 5;

    /**
     * ?游?＊蝷箸芋撘?蝢押?     */
    public enum DisplayMode {
        /** ?＊蝷粹?獢?*/
        EDGES_ONLY,
        /** 摰憿舐內 (?? + 摨扳?頠?+ 璅惜)??*/
        FULL,
        /** ?＊蝷箸?蝐扎?*/
        LABELS_ONLY;

        public String token() {
            return switch (this) {
                case EDGES_ONLY -> "edges-only";
                case FULL -> "full";
                case LABELS_ONLY -> "labels-only";
            };
        }

        public String id() {
            return token();
        }

        public static DisplayMode parse(String raw) {
            String token = raw == null ? "" : raw.trim().toLowerCase().replace('_', '-');
            return switch (token) {
                case "edges", "edges-only" -> EDGES_ONLY;
                case "full" -> FULL;
                case "labels", "labels-only" -> LABELS_ONLY;
                default -> throw new IllegalArgumentException("Unknown display mode: " + raw);
            };
        }
    }

    private FieldVisualizationManager() {
    }

    /**
     * 摰?閬死?頂蝯梧???隡箸???Tick 鈭辣??     */
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

    /**
     * ?箸?摰摰嗅??典?啗?閬箏???     */
    public void enable(UUID playerId) {
        ENABLED.add(playerId);
        PLAYER_RADIUS.putIfAbsent(playerId, 64);
        PLAYER_STYLE.putIfAbsent(playerId, HologramStyle.defaults());
        PLAYER_MODE.putIfAbsent(playerId, DisplayMode.EDGES_ONLY);
    }

    /**
     * ?箸?摰摰嗅??典?啗?閬箏???     */
    public void disable(UUID playerId) {
        ENABLED.remove(playerId);
        LAST_SYNC_TICK.remove(playerId);
        PLAYER_RADIUS.remove(playerId);
        PLAYER_STYLE.remove(playerId);
        PLAYER_MODE.remove(playerId);
    }

    /**
     * 閮剖??拙振?＊蝷箸芋撘?     */
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

    /**
     * ???孵??冽?蔣???????     */
    public void setFeature(UUID playerId, HologramFeature feature, boolean enabled) {
        HologramStyle current = getStyle(playerId);
        PLAYER_STYLE.put(playerId, current.withFeature(feature, enabled));
    }

    /**
     * ?箇摰嗆??蒂?郊???閬?舀?敶梧??游 + ?函??蔣嚗?     */
    private void renderForPlayer(ServerLevel level, ServerPlayer player, int tick) {
        Integer last = LAST_SYNC_TICK.getOrDefault(player.getUUID(), 0);
        if (tick - last < SYNC_INTERVAL_TICKS) return;
        LAST_SYNC_TICK.put(player.getUUID(), tick);

        Vec3 viewer = player.position();
        int radius = PLAYER_RADIUS.getOrDefault(player.getUUID(), 64);
        HologramStyle style = getStyle(player.getUUID());
        List<HologramDefinition> visible = new ArrayList<>();

        // 1. ??銝行溶??餈??游 (Field)
        for (FieldDefinition field : FieldManager.INSTANCE.all().values()) {
            if (!field.dimensionId().equals(level.dimension().identifier())) continue;
            if (distanceToAabb(viewer, field.bounds()) > radius) continue;

            visible.add(new HologramDefinition(
                    field.uuid(),
                    field.dimensionId(),
                    field.bounds(),
                    field.uuid().toString(),
                    style
            ));
        }

        // 2. ??銝行溶?蝡??冽?蔣 (Hologram)
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
