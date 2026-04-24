package com.myudog.myulib.api.ui.tablist;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 管理伺服器的 Tab 面板 (Header/Footer 與玩家稱號)
 */
public final class TabListManager {

    public static final TabListManager INSTANCE = new TabListManager();

    private TabListManager() {}

    private Component currentHeader = Component.empty();
    private Component currentFooter = Component.empty();

    public void install() {
        // 定期更新所有玩家的 Tab 顯示名稱與權重
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 實務上可以每 20 Ticks (1秒) 更新一次即可，避免浪費效能
            if (server.getTickCount() % 20 == 0) {
                updateAllPlayersDisplayName(server.getPlayerList().getPlayers());
            }
        });
    }

    /**
     * 設定並廣播 Tab 面板的上下標語
     */
    public void setHeaderFooter(List<ServerPlayer> targets, Component header, Component footer) {
        currentHeader = header;
        currentFooter = footer;

        ClientboundTabListPacket packet = new ClientboundTabListPacket(header, footer);
        for (ServerPlayer player : targets) {
            player.connection.send(packet);
        }
    }

    /**
     * 動態結合 RoleGroup 更新玩家在 Tab 中的顯示名稱與排序
     */
    public void updateAllPlayersDisplayName(List<ServerPlayer> allPlayers) {
        for (ServerPlayer player : allPlayers) {

            // 1. 從我們剛寫好的 RoleGroupManager 獲取該玩家最高權重的身分組
            List<String> groupIds = RoleGroupManager.INSTANCE.getSortedGroupIdsOf(player.getUUID());
            RoleGroupDefinition highestGroup = null;
            if (!groupIds.isEmpty()) {
                highestGroup = RoleGroupManager.INSTANCE.get(parseGroupId(groupIds.get(0)));
            }

            // 2. 組合顯示名稱 (例如: "[管理員] Chiayu")
            Component displayName = Component.literal(player.getName().getString());
            if (highestGroup != null) {
                displayName = Component.empty()
                        .append(Component.literal("[").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(highestGroup.translationKey())
                        .append(Component.literal("] ").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(displayName);
            }

            // 3. 設定原版的 TabList 名稱
            player.getTabListDisplayName(); // 原版機制

            // 🌟 4. 發送更新封包給所有人，讓大家看到他的新名字
            // 我們不需要自己寫迴圈，直接生成一個 UPDATE_DISPLAY_NAME 的封包並廣播
            ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player
            );

            for (ServerPlayer viewer : allPlayers) {
                viewer.connection.send(packet);
            }
        }
    }

    private Identifier parseGroupId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "everyone");
        }
        if (rawId.contains(":")) {
            return Identifier.parse(rawId);
        }
        return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, rawId);
    }
}