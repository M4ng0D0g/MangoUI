package com.myudog.myulib.api.hologram;

import com.myudog.myulib.api.hologram.network.HologramNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramManager {

    // 🌟 1. 唯一實例 (Eager Initialization)
    public static final HologramManager INSTANCE = new HologramManager();

    // 🌟 2. 移除 static 修飾符，變成實例變數
    private final Map<Identifier, HologramDefinition> registry = new ConcurrentHashMap<>();

    // 🌟 3. 私有化建構子，防止外部 new
    private HologramManager() {}

    public void register(HologramDefinition definition) {
        registry.put(definition.id(), definition);
    }

    public void unregister(Identifier id) {
        registry.remove(id);
    }

    public HologramDefinition get(Identifier id) {
        return registry.get(id);
    }

    public AABB cuboidFromCorners(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AABB(
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.min(z1, z2),
                Math.max(x1, x2),
                Math.max(y1, y2),
                Math.max(z1, z2)
        );
    }

    /**
     * 🌟 將特定投影同步給指定玩家 (實現選擇性渲染)
     */
    public void updatePlayerView(ServerPlayer player, List<Identifier> ids) {
        List<HologramDefinition> toSync = registry.entrySet().stream()
                .filter(e -> ids.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        HologramNetworking.syncToPlayer(player, toSync);
    }

    public Map<Identifier, HologramDefinition> all() {
        return Map.copyOf(registry);
    }

    public void clearForPlayer(ServerPlayer player) {
        HologramNetworking.syncToPlayer(player, List.of());
    }
}