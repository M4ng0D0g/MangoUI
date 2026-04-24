package com.myudog.myulib.api.debug;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DebugLogManager {

    public static final DebugLogManager INSTANCE = new DebugLogManager();

    
    private final Set<UUID> ENABLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private final Map<UUID, EnumSet<DebugFeature>> PLAYER_FEATURES = new ConcurrentHashMap<>();
    private volatile MinecraftServer server;
    private volatile boolean installed;

    private DebugLogManager() {
    }

    public void install() {
        if (installed) {
            return;
        }
        installed = true;
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            server = null;
            ENABLED_PLAYERS.clear();
            PLAYER_FEATURES.clear();
        });
    }

    public void enable(UUID playerId) {
        if (playerId == null) {
            return;
        }
        ENABLED_PLAYERS.add(playerId);
        PLAYER_FEATURES.computeIfAbsent(playerId, ignored -> EnumSet.allOf(DebugFeature.class));
    }

    public void disable(UUID playerId) {
        if (playerId == null) {
            return;
        }
        ENABLED_PLAYERS.remove(playerId);
    }

    public boolean isEnabled(UUID playerId) {
        return playerId != null && ENABLED_PLAYERS.contains(playerId);
    }

    public void setFeature(UUID playerId, DebugFeature feature, boolean enabled) {
        if (playerId == null || feature == null) {
            return;
        }
        EnumSet<DebugFeature> set = PLAYER_FEATURES.computeIfAbsent(playerId, ignored -> EnumSet.noneOf(DebugFeature.class));
        if (enabled) {
            set.add(feature);
        } else {
            set.remove(feature);
        }
    }

    public void setAll(UUID playerId, boolean enabled) {
        if (playerId == null) {
            return;
        }
        if (enabled) {
            PLAYER_FEATURES.put(playerId, EnumSet.allOf(DebugFeature.class));
        } else {
            PLAYER_FEATURES.put(playerId, EnumSet.noneOf(DebugFeature.class));
        }
    }

    public Set<DebugFeature> getFeatures(UUID playerId) {
        EnumSet<DebugFeature> set = PLAYER_FEATURES.get(playerId);
        if (set == null) {
            return Set.of();
        }
        return Set.copyOf(set);
    }

    public void log(DebugFeature feature, String message) {
        MinecraftServer current = server;
        if (current == null || feature == null || message == null || message.isBlank()) {
            return;
        }

        for (UUID playerId : Set.copyOf(ENABLED_PLAYERS)) {
            ServerPlayer player = current.getPlayerList().getPlayer(playerId);
            if (player == null) {
                ENABLED_PLAYERS.remove(playerId);
                continue;
            }
            EnumSet<DebugFeature> features = PLAYER_FEATURES.get(playerId);
            if (features == null || !features.contains(feature)) {
                continue;
            }
            player.sendSystemMessage(Component.literal("[Debug:" + feature.token() + "] " + message));
        }
    }
}

