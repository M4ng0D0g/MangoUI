package com.myudog.myulib.api.core.hologram;

import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import com.myudog.myulib.api.core.hologram.network.HologramNetworking;
import com.myudog.myulib.api.core.hologram.storage.NbtHologramStorage;
import com.myudog.myulib.api.core.storage.DataStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HologramManager
 *
 * 系統：核心全息投影系統 (Core - Hologram)
 * 角色：管理伺服器端的全息投影定義、持久化與同步路由。
 * 類型：Manager / Sync Provider
 *
 * `HologramManager` 負責維護伺服器端所有的全息投影資料。
 * 它並不直接處理渲染，而是將 {@link HologramDefinition} 透過 {@link HologramNetworking} 發送至客戶端。
 * 客戶端接收後會根據樣式設定進行 3D 渲染（如 AABB 框線、文字標籤等）。
 */
public final class HologramManager {

    /** 唯一實例。 */
    public static final HologramManager INSTANCE = new HologramManager();

    /** 依賴注入的儲存庫。 */
    private DataStorage<UUID, HologramDefinition> storage;

    {
        storage = new NbtHologramStorage();
    }

    /** 記憶體快取註冊表。 */
    private final Map<UUID, HologramDefinition> registry = new ConcurrentHashMap<>();

    private HologramManager() {
    }

    /**
     * 允許在初始化階段抽換儲存策略 (IoC)。
     */
    public void setStorage(DataStorage<UUID, HologramDefinition> storage) {
        this.storage = storage;
    }

    /**
     * 在伺服器啟動階段載入所有全息投影資料。
     */
    public void load(MinecraftServer server) {
        storage.initialize(server);
        registry.clear();
        registry.putAll(storage.loadAll());
        DebugLogManager.INSTANCE.log(DebugFeature.HOLOGRAM,
                "load: count=" + registry.size());
    }

    /**
     * 註冊並保存一個新的全息投影。
     */
    public void register(HologramDefinition definition) {
        registry.put(definition.uuid(), definition);
        storage.save(definition.uuid(), definition);
        DebugLogManager.INSTANCE.log(DebugFeature.HOLOGRAM,
                "register: id=" + definition.id() + ",uuid=" + definition.uuid());
    }

    /**
     * 註銷並刪除指定的全息投影。
     */
    public void unregister(UUID uuid) {
        registry.remove(uuid);
        storage.delete(uuid);
        DebugLogManager.INSTANCE.log(DebugFeature.HOLOGRAM,
                "unregister: uuid=" + uuid);
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

    /** 獲取所有已註冊的全息投影。 */
    public Map<UUID, HologramDefinition> all() {
        return Map.copyOf(registry);
    }

    /**
     * 將指定的投影資料同步給特定玩家。
     *
     * @param player 目標玩家
     * @param uuids  要同步的投影 UUID 清單
     */
    public void updatePlayerView(ServerPlayer player, List<UUID> uuids) {
        List<HologramDefinition> toSync = registry.entrySet().stream()
                .filter(e -> uuids.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        HologramNetworking.syncToPlayer(player, toSync);
        DebugLogManager.INSTANCE.log(DebugFeature.HOLOGRAM,
                "sync: player=" + player.getName().getString() + ",count=" + toSync.size());
    }

    /**
     * 清除玩家客戶端的所有全息投影。
     */
    public void clearForPlayer(ServerPlayer player) {
        HologramNetworking.syncToPlayer(player, List.of());
        DebugLogManager.INSTANCE.log(DebugFeature.HOLOGRAM,
                "clear: player=" + player.getName().getString());
    }

    /**
     * 輔助工具：從兩個對角座標建立 AABB。
     */
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
