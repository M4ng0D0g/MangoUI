package com.myudog.myulib.api.hologram;

import com.myudog.myulib.api.hologram.network.HologramNetworking;
import com.myudog.myulib.api.hologram.storage.NbtHologramStorage;
import com.myudog.myulib.api.storage.DataStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramManager {

    // 🌟 唯一實例
    public static final HologramManager INSTANCE = new HologramManager();

    // 依賴注入的儲存庫 (預設為 NBT 世界綁定)
    private DataStorage<UUID, HologramDefinition> storage;

    {
        storage = new NbtHologramStorage();
    }

    // 記憶體快取
    private final Map<UUID, HologramDefinition> registry = new ConcurrentHashMap<>();

    private HologramManager() {}

    /**
     * 允許在初始化階段抽換儲存策略 (IoC)
     */
    public void setStorage(DataStorage<UUID, HologramDefinition> storage) {
        this.storage = storage;
    }

    /**
     * 在 ServerStarting 事件中被呼叫，讀取所有 NBT 到記憶體
     */
    public void load(MinecraftServer server) {
        storage.initialize(server);
        registry.clear();
        registry.putAll(storage.loadAll());
        System.out.println("[Myulib] Hologram 系統初始化完成，載入 " + registry.size() + " 筆資料。");
    }

    // --- 核心 CRUD 與同步 ---

    public void register(HologramDefinition definition) {
        registry.put(definition.uuid(), definition);
        storage.save(definition.uuid(), definition);
    }

    public void unregister(UUID uuid) {
        registry.remove(uuid);
        storage.delete(uuid);
    }

    public void unregister(net.minecraft.resources.Identifier id) {
        unregister(stableUuid(id.toString()));
    }

    public HologramDefinition get(UUID uuid) {
        return registry.get(uuid);
    }

    public HologramDefinition get(net.minecraft.resources.Identifier id) {
        return registry.get(stableUuid(id.toString()));
    }

    public Map<UUID, HologramDefinition> all() {
        return Map.copyOf(registry);
    }

    /**
     * 將特定投影同步給指定玩家
     */
    public void updatePlayerView(ServerPlayer player, List<UUID> uuids) {
        List<HologramDefinition> toSync = registry.entrySet().stream()
                .filter(e -> uuids.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        HologramNetworking.syncToPlayer(player, toSync);
    }

    public void clearForPlayer(ServerPlayer player) {
        HologramNetworking.syncToPlayer(player, List.of());
    }

    // --- 工具 ---
    public static AABB cuboidFromCorners(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AABB(
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)
        );
    }

    private static UUID stableUuid(String token) {
        return UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8));
    }
}