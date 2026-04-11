package com.myudog.myulib.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class AccessCommandService {

    public static void registerDefaults() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // 這裡呼叫了下方實作的指令樹
            registerMyulibBaseCommand(dispatcher);
        });
    }

    /**
     * 🛠️ 實作基礎的 /myulib 指令樹 (解決 cannot find symbol 錯誤)
     */
    private static void registerMyulibBaseCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("myulib")
                        .requires(source -> source.permissions().hasPermission(
                                new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)
                        )) // 需要管理員權限 (OP 等級 2)

                        // 子指令：/myulib save (強制手動存檔)
                        .then(Commands.literal("save")
                                .executes(context -> {
                                    PermissionManager.save();
                                    FieldManager.save();
                                    RoleGroupManager.save();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("§a[Myulib] 所有資料已成功手動寫入 NBT 存檔！"),
                                            true
                                    );
                                    return 1;
                                })
                        )
                // 未來你的 `/myulib group create <id>` 等指令都可以繼續掛在這裡
                // 並在 executes 內呼叫下方的 Service 方法
        );
    }

    // ====================================================================
    // 以下為內部服務封裝 (Service Layer)，供指令或外部 API 呼叫
    // ====================================================================

    // --- RoleGroup (身分組) ---
    public static void createRoleGroup(String groupId, String displayName, int priority) {
        RoleGroupDefinition group = new RoleGroupDefinition(groupId, displayName, priority, Map.of(), Set.of());
        RoleGroupManager.register(group);
        RoleGroupManager.save(); // 💡 新增資料後觸發存檔
    }

    public static void deleteRoleGroup(String groupId) {
        RoleGroupManager.delete(groupId);
        RoleGroupManager.save(); // 💡 刪除資料後觸發存檔
    }

    public static List<RoleGroupDefinition> listRoleGroups() {
        return RoleGroupManager.groups();
    }

    // --- Permission (權限) ---
    public static void grantGlobalPermission(String groupId, PermissionAction action, PermissionDecision decision) {
        PermissionManager.global().forGroup(groupId).set(action, decision);
        PermissionManager.save();
    }

    // --- Field (保護區) ---
    public static void createField(FieldDefinition field) {
        FieldManager.register(field);
        // FieldManager 內部的 NbtFieldStorage 在 add() 時預設已經會 markDirty()
        // 但安全起見可以統一呼叫，不會有額外效能負擔
        FieldManager.save();
    }

    public static void deleteField(Identifier fieldId) {
        FieldManager.unregister(fieldId);
        FieldManager.save();
    }
}