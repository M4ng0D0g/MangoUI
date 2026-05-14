package com.myudog.myulib.api.framework.ui;

import com.myudog.myulib.api.framework.permission.ScopeLayer;
import net.minecraft.resources.Identifier;

/**
 * ConfigurationUiRegistry
 *
 * 蝟餌絞嚗?蝵桐??Ｙ頂蝯?(Framework - UI)
 * 閫嚗??箸??嗅惜?擃?UI 撖虫?銋???璅?
 * 憿?嚗egistry / Service Locator
 *
 * ?望 UI 撖虫??虜雿 Client ?湔??函?撠?銝哨?獢撅日? {@link ConfigurationUiBridge}
 * 隞亥圾?衣??孵??澆?車蝺刻摩?剁?憒?啁楊頛臬???楊頛臬蝑???
 */
public final class ConfigurationUiRegistry {
    private static volatile ConfigurationUiBridge bridge;

    private ConfigurationUiRegistry() {
    }

    /**
     * 閮剖? UI 璈?典祕雿?
     */
    public static void setBridge(ConfigurationUiBridge bridge) {
        ConfigurationUiRegistry.bridge = bridge;
    }

    /**
     * ?脣??嗅???UI 璈?具?
     */
    public static ConfigurationUiBridge bridge() {
        return bridge;
    }

    /**
     * ???孵??游?楊頛臭??Ｕ?
     */
    public static void openFieldEditor(Identifier fieldId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openFieldEditor(fieldId);
        }
    }

    /**
     * ??頨怠?蝯?(Identity Group) ?楊頛臭??Ｕ?
     */
    public static void openIdentityGroupEditor(Identifier groupId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openIdentityGroupEditor(groupId);
        }
    }

    /**
     * ??閫蝯?(Role Group) ?楊頛臭??Ｕ?
     */
    public static void openRoleGroupEditor(Identifier groupId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openRoleGroupEditor(groupId);
        }
    }

    /**
     * ???? (Team) ?楊頛臭??Ｕ?
     */
    public static void openTeamEditor(Identifier teamId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openTeamEditor(teamId);
        }
    }

    /**
     * ???孵?雿?惜蝝?甈?蝺刻摩隞??
     *
     * @param layer   雿?惜蝝?(GLOBAL, DIMENSION, FIELD)
     * @param scopeId 雿??霅泵
     */
    public static void openPermissionEditor(ScopeLayer layer, String scopeId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openPermissionEditor(layer, scopeId);
        }
    }
}
