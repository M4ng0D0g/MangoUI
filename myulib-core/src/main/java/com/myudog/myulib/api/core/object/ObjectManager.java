package com.myudog.myulib.api.core.object;

import com.myudog.myulib.api.core.event.EventBus;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import com.myudog.myulib.api.core.object.event.BlockBreakEvent;
import com.myudog.myulib.api.core.object.event.BlockInteractEvent;
import com.myudog.myulib.api.core.object.event.EntityDamageEvent;
import com.myudog.myulib.api.core.object.event.EntityDeathEvent;
import com.myudog.myulib.api.core.object.event.EntityInteractEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ObjectManager
 *
 * 系統：遊戲物件系統 (Object & Behavior System)
 * 角色：全域物件管理與原生事件攔截中心。負責維護物件定義、追蹤運行時物件實例，並將 Minecraft 原生事件轉發為系統事件。
 * 類型：Manager / Event Dispatcher
 *
 * 此類別設計為與具體的遊戲邏輯（如 GameInstance）完全解耦，允許任何系統透過註冊定義或監聽 nativeEventBus 
 * 來擴充遊戲物件的行為。Mixin 會在關鍵點（如受傷、死亡、互動）呼叫此類別的方法進行事件分發。
 */
public final class ObjectManager {

    public static final ObjectManager INSTANCE = new ObjectManager();

    private final Map<Identifier, IObjectDef> definitions = new ConcurrentHashMap<>();
    
    /**
     * 原生事件全域廣播中心。
     * 所有的實體受傷、死亡、方塊破壞等事件都會經過此匯流排。
     */
    private final EventBus nativeEventBus = new EventBus();
    
    private final Map<UUID, IObjectRt> entityObjects = new ConcurrentHashMap<>();
    private final Map<BlockPos, IObjectRt> blockObjects = new ConcurrentHashMap<>();

    private ObjectManager() {}

    /**
     * 獲取原生事件匯流排。
     *
     * @return 核心事件匯流排實例
     */
    public EventBus getNativeEventBus() {
        return nativeEventBus;
    }

    /**
     * 註冊一個物件定義。
     *
     * @param id  定義的唯一識別碼 (Identifier)
     * @param def 物件定義實作
     */
    public void registerDefinition(Identifier id, IObjectDef def) {
        definitions.put(id, def);
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "register def=" + id);
    }

    /**
     * 根據識別碼獲取物件定義。
     *
     * @param defId 定義 ID
     * @return 物件定義，若不存在則為 null
     */
    public IObjectDef getDefinition(Identifier defId) {
        return definitions.get(defId);
    }

    // --- 物件註冊 API ---

    /**
     * 將一個運行時物件與實體 UUID 關聯。
     */
    public void registerEntityObject(UUID entityUuid, IObjectRt obj) {
        entityObjects.put(entityUuid, obj);
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "register entity=" + entityUuid);
    }
    public void unregisterEntityObject(UUID entityUuid) {
        entityObjects.remove(entityUuid);
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "unregister entity=" + entityUuid);
    }

    /**
     * 將一個運行時物件與方塊座標關聯。
     */
    public void registerBlockObject(BlockPos pos, IObjectRt obj) {
        blockObjects.put(pos, obj);
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "register block=" + pos.toShortString());
    }
    public void unregisterBlockObject(BlockPos pos) {
        blockObjects.remove(pos);
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "unregister block=" + pos.toShortString());
    }

    // --- 事件攔截與分發 (供 Mixin 呼叫) ---

    /**
     * 處理實體受傷事件。
     * 由 Mixin 呼叫，並派發 EntityDamageEvent。
     *
     * @param victim 被攻擊者
     * @param source 傷害來源
     * @param amount 傷害數值
     * @return 若事件被取消則為 true
     */
    public boolean handleEntityDamage(LivingEntity victim, DamageSource source, float amount) {
        EntityDamageEvent event = new EntityDamageEvent(victim, source, amount);
        nativeEventBus.dispatch(event);
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "event damage victim=" + victim.getUUID() + ",amount=" + amount + ",canceled=" + event.isCanceled());
        return event.isCanceled();
    }

    /**
     * 處理實體死亡事件。
     */
    public void handleEntityDeath(LivingEntity victim, DamageSource source) {
        EntityDeathEvent event = new EntityDeathEvent(victim, source);
        nativeEventBus.dispatch(event);
        entityObjects.remove(victim.getUUID());
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "event death victim=" + victim.getUUID());
    }

    /**
     * 處理玩家與實體的互動事件。
     */
    public boolean handleEntityInteract(ServerPlayer player, Entity target, InteractionHand hand) {
        EntityInteractEvent event = new EntityInteractEvent(player, target, hand);
        nativeEventBus.dispatch(event);
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "event interact player=" + player.getName().getString() + ",target=" + target.getUUID() + ",hand=" + hand + ",canceled=" + event.isCanceled());
        return event.isCanceled();
    }

    /**
     * 處理方塊破壞事件。
     */
    public boolean handleBlockBreak(ServerPlayer player, BlockPos pos, ServerLevel level) {
        BlockBreakEvent event = new BlockBreakEvent(player, pos, level);
        nativeEventBus.dispatch(event);
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "event block-break player=" + player.getName().getString() + ",pos=" + pos.toShortString() + ",canceled=" + event.isCanceled());
        return event.isCanceled();
    }

    /**
     * 處理方塊互動事件。
     */
    public boolean handleBlockInteract(ServerPlayer player, BlockPos pos, ServerLevel level) {
        BlockInteractEvent event = new BlockInteractEvent(player, pos, level);
        nativeEventBus.dispatch(event);
        DebugLogManager.INSTANCE.log(DebugFeature.OBJECT,
                "event block-interact player=" + player.getName().getString() + ",pos=" + pos.toShortString() + ",canceled=" + event.isCanceled());
        return event.isCanceled();
    }
}