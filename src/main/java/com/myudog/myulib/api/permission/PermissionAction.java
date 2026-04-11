package com.myudog.myulib.api.permission;

public enum PermissionAction {
    // --- 方塊與環境 ---
    BLOCK_PLACE,         // 放置方塊
    BLOCK_BREAK,         // 破壞方塊
    INTERACT_BLOCK,      // 互動方塊 (開門、按按鈕)
    USE_BUCKET,          // 🪣 新增：使用水桶/岩漿桶 (防倒岩漿破壞)
    IGNITE_BLOCK,        // 🔥 新增：點火 (打火石、火焰彈)
    TRAMPLE_FARMLAND,    // 🌾 新增：踩壞農田

    // --- 實體與戰鬥 ---
    ATTACK_PLAYER,       // 攻擊玩家 (PVP)
    ATTACK_FRIENDLY_MOB, // 攻擊友善生物 (村民、動物)
    ATTACK_HOSTILE_MOB,  // 攻擊敵對生物 (殭屍、苦力怕)
    INTERACT_ENTITY,     // 互動實體 (跟村民交易、騎馬)
    RIDE_ENTITY,         // 🐎 新增：騎乘實體 (礦車、船、馬)
    USE_SPAWN_EGG,       // 🥚 新增：使用生怪蛋
    ARMOR_STAND_MANIPULATE, // 🧍 新增：操作盔甲架 (防偷裝備)

    // --- 物品與系統 ---
    USE_PROJECTILE,      // 發射投射物 (丟雪球、射箭)
    USE_ITEM,            // 使用物品 (吃食物、喝藥水)
    DROP_ITEM,           // 丟棄物品
    PICKUP_ITEM,         // 🎒 新增：撿起物品
    OPEN_CONTAINER,      // 📦 新增：打開容器 (箱子、漏斗) - 從 INTERACT_BLOCK 獨立出來更安全
    TRIGGER_REDSTONE,    // 觸發紅石
    USE_PORTAL,          // 使用傳送門
    SEND_MESSAGE         // 發送訊息
}