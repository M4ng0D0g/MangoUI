package com.myudog.myulib.internal.field;

import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;

/**
 * 內部服務：專門處理來自遊戲內指令 (Command) 或 GUI 的區域管理請求
 */
public class FieldAdminService {

    // 舉例：透過管理員指令建立新區域
    public static boolean createFieldViaCommand(ServerPlayer admin, FieldDefinition field) {
        // 1. 這裡可以做額外的業務邏輯檢查（例如：管理員權限檢查）
        return false;
//        if (!admin.hasPermissionLevel(2)) { // 假設 2 級以上是管理員
//            return false;
//        }

//        try {
//            // 2. 呼叫我們唯一且核心的 FieldManager 進行註冊
//            FieldManager.INSTANCE.register(field);
//            return true;
//        } catch (IllegalArgumentException e) {
//            // 捕捉重疊錯誤，回傳給指令系統
//            return false;
//        }
    }
}