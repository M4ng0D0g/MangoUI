package com.myudog.myulib.api.framework.permission;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.MyulibFramework;
import com.myudog.myulib.api.framework.field.FieldDefinition;
import com.myudog.myulib.api.framework.field.FieldManager;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import com.myudog.myulib.api.framework.rolegroup.RoleGroupManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * PermissionGate
 *
 * 系統：權限管理系統 (Framework - Permission)
 * 角色：權限判定的守門員，整合位置、身分組與權限表來決定玩家是否能執行動作。
 * 類型：Utility / Facade
 *
 * PermissionGate 串聯了多個系統：
 * 1. 透過 FieldManager 獲取目前座標所屬的場地。
 * 2. 透過 RoleGroupManager 獲取玩家具備的角色組。
 * 3. 透過 PermissionManager 執行最終的權限判定。
 */
public final class PermissionGate {
    private PermissionGate() {
    }

    /**
     * 檢查玩家是否被禁止執行特定動作。
     *
     * @param player         玩家實例
     * @param action         權限動作
     * @param targetPosition 目標位置（若為 null，預設為玩家目前位置）
     * @return true 若被禁止 (DENY)
     */
    public static boolean isDenied(Player player, PermissionAction action, Vec3 targetPosition) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return isDenied(serverPlayer, action, targetPosition);
    }

    /**
     * @see #isDenied(Player, PermissionAction, Vec3)
     */
    public static boolean isDenied(ServerPlayer player, PermissionAction action, Vec3 targetPosition) {
        return evaluateDecision(player, action, targetPosition) == PermissionDecision.DENY;
    }

    /**
     * 評估玩家在特定位置執行動作的權限結果。
     *
     * @param player         玩家實例
     * @param action         權限動作
     * @param targetPosition 目標位置
     * @return 最終結果 (ALLOW, DENY, UNSET)
     */
    public static PermissionDecision evaluateDecision(ServerPlayer player, PermissionAction action, Vec3 targetPosition) {
        if (player == null || action == null) {
            return PermissionDecision.UNSET;
        }

        Vec3 resolvedPosition = targetPosition == null ? player.position() : targetPosition;
        Identifier dimId = player.level().dimension().identifier();
        Optional<FieldDefinition> field = FieldManager.INSTANCE.findAt(dimId, resolvedPosition);
        List<String> groups = RoleGroupManager.INSTANCE.getSortedGroupIdsOf(player.getUUID());

        // 將場地 UUID 轉為 Identifier 以利於權限比對
        Identifier fieldIdentifier = field.map(fieldDefinition -> Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "field/" + fieldDefinition.uuid())).orElse(null);

        PermissionDecision decision = PermissionManager.INSTANCE.evaluate(
                player.getUUID(),
                groups,
                action,
                fieldIdentifier,
                dimId
        );

        // 特殊處理：如果動作是類互動型動作，但 USE_ITEM 被禁止，則結果也是 DENY
        if (decision != PermissionDecision.DENY && action != PermissionAction.USE_PORTAL && action != PermissionAction.USE_ITEM && isUseLikeAction(action)) {
            PermissionDecision useItemDecision = PermissionManager.INSTANCE.evaluate(
                    player.getUUID(),
                    groups,
                    PermissionAction.USE_ITEM,
                    fieldIdentifier,
                    dimId
            );
            if (useItemDecision == PermissionDecision.DENY) {
                decision = PermissionDecision.DENY;
            }
        }

        // 記錄權限判定日誌
        DebugLogManager.INSTANCE.log(DebugFeature.PERMISSION,
                "player=" + player.getName().getString()
                        + ",action=" + action
                        + ",decision=" + decision
                        + ",groups=" + String.join("|", groups)
                        + ",field=" + (fieldIdentifier != null ? fieldIdentifier.toString() : "-")
                        + ",dim=" + dimId
                        + ",pos=(" + String.format("%.2f", resolvedPosition.x) + ","
                        + String.format("%.2f", resolvedPosition.y) + ","
                        + String.format("%.2f", resolvedPosition.z) + ")");

        return decision;
    }

    private static boolean isUseLikeAction(PermissionAction action) {
        return switch (action) {
            case BLOCK_PLACE,
                 INTERACT_BLOCK,
                 USE_BUCKET,
                 IGNITE_BLOCK,
                 INTERACT_ENTITY,
                 RIDE_ENTITY,
                 USE_SPAWN_EGG,
                 ARMOR_STAND_MANIPULATE,
                 USE_PROJECTILE,
                 OPEN_CONTAINER,
                 TRIGGER_REDSTONE -> true;
            default -> false;
        };
    }
}
