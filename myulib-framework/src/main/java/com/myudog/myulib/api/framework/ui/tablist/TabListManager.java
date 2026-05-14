package com.myudog.myulib.api.framework.ui.tablist;

import com.myudog.myulib.Myulib;

import com.myudog.myulib.api.framework.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.framework.rolegroup.RoleGroupManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 蝞∠?隡箸??函? Tab ?Ｘ (Header/Footer ?摰嗥迂??
 */
public final class TabListManager {

    public static final TabListManager INSTANCE = new TabListManager();

    private TabListManager() {}

    private Component currentHeader = Component.empty();
    private Component currentFooter = Component.empty();

    public void install() {
        // 摰??湔??摰嗥? Tab 憿舐內?迂????
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 撖血?銝隞交? 20 Ticks (1蝘? ?湔銝甈∪?荔??踹?瘚芾祥?
            if (server.getTickCount() % 20 == 0) {
                updateAllPlayersDisplayName(server.getPlayerList().getPlayers());
            }
        });
    }

    /**
     * 閮剖?銝血誨??Tab ?Ｘ??銝?隤?
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
     * ??蝯? RoleGroup ?湔?拙振??Tab 銝剔?憿舐內?迂??摨?
     */
    public void updateAllPlayersDisplayName(List<ServerPlayer> allPlayers) {
        for (ServerPlayer player : allPlayers) {

            // 1. 敺???撖怠末??RoleGroupManager ?脣?閰脩摰嗆?擃???頨怠?蝯?
            List<String> groupIds = RoleGroupManager.INSTANCE.getSortedGroupIdsOf(player.getUUID());
            RoleGroupDefinition highestGroup = null;
            if (!groupIds.isEmpty()) {
                highestGroup = RoleGroupManager.INSTANCE.get(parseGroupId(groupIds.get(0)));
            }

            // 2. 蝯?憿舐內?迂 (靘?: "[蝞∠??（ Chiayu")
            Component displayName = Component.literal(player.getName().getString());
            if (highestGroup != null) {
                displayName = Component.empty()
                        .append(Component.literal("[").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(highestGroup.translationKey())
                        .append(Component.literal("] ").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(displayName);
            }

            // 3. 閮剖?????TabList ?迂
            player.getTabListDisplayName(); // ??璈

            // ?? 4. ?潮?啣??策??犖嚗?憭批振?隞??啣?摮?
            // ???閬撌勗神餈游?嚗?亦?????UPDATE_DISPLAY_NAME ???蒂撱?
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
