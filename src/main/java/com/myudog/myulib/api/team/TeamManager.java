package com.myudog.myulib.api.team;

import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import com.myudog.myulib.api.storage.DataStorage;
import com.myudog.myulib.api.team.storage.NbtTeamStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class TeamManager {

    public static final TeamManager INSTANCE = new TeamManager();

    private final Map<UUID, TeamDefinition> TEAMS = new LinkedHashMap<>();

    private final Map<UUID, Set<UUID>> TEAM_MEMBERS = new LinkedHashMap<>();
    private final Map<UUID, UUID> MEMBER_TEAM = new LinkedHashMap<>();

    private final Map<UUID, UUID> TEAM_GAME = new LinkedHashMap<>();
    private MinecraftServer currentServer;

    // 🌟 儲存庫實例
    private DataStorage<UUID, TeamDefinition> storage;

    private TeamManager() {
    }

    public boolean hasTeam(@NotNull UUID requestedTeam) {
        return TEAMS.containsKey(requestedTeam);
    }

    public TeamDefinition getDefinition(@NotNull UUID teamId) {
        return TEAMS.get(teamId);
    }

    public void install() {
        install(new NbtTeamStorage());
    }

    /**
     * 🌟 依賴注入：在模組啟動時，傳入指定的儲存庫實作
     */
    public void install(DataStorage<UUID, TeamDefinition> storageProvider) {
        storage = storageProvider;

        // 掛載伺服器啟動事件，初始化儲存庫並載入所有資料
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            currentServer = server;
            if (storage != null) {
                storage.initialize(server);

                // 將儲存庫中的資料載入記憶體
                Map<UUID, TeamDefinition> loadedData = storage.loadAll();
                TEAMS.clear();
                if (loadedData != null) {
                    TEAMS.putAll(loadedData);
                }
                syncAllNativeTeams(server);
                System.out.println("[Myulib] TeamManager 已成功載入 " + TEAMS.size() + " 筆隊伍資料。");
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);
    }

    public TeamDefinition register(TeamDefinition team) {
        Objects.requireNonNull(team, "team");
        if (!validate(team)) {
            throw new IllegalArgumentException("TeamDefinition 驗證失敗: " + team.uuid());
        }
        TEAMS.put(team.uuid(), team);
        TEAM_MEMBERS.computeIfAbsent(team.uuid(), ignored -> new LinkedHashSet<>());
        TEAM_GAME.remove(team.uuid());

        // 🌟 寫入儲存庫
        if (storage != null) {
            storage.save(team.uuid(), team);
        }

        DebugLogManager.INSTANCE.log(DebugFeature.TEAM,
                "register id=" + team.uuid() + ",color=" + team.color());

        syncNativeTeam(currentServer, team.uuid());

        return team;
    }

    public TeamDefinition register(Identifier gameId, TeamDefinition team) {
        return register(team);
    }

    public boolean validate(TeamDefinition team) {
        return team != null && team.playerLimit() >= 0 && !TEAMS.containsKey(team.uuid());
    }

    public TeamDefinition update(UUID teamId, UnaryOperator<TeamDefinition> updater) {
        Objects.requireNonNull(teamId, "teamId");
        Objects.requireNonNull(updater, "updater");
        TeamDefinition existing = TEAMS.get(teamId);
        if (existing == null) {
            return null;
        }
        TeamDefinition updated = Objects.requireNonNull(updater.apply(existing), "updated team");
        TEAMS.put(teamId, updated);

        // 🌟 更新儲存庫
        if (storage != null) {
            storage.save(teamId, updated);
        }

        DebugLogManager.INSTANCE.log(DebugFeature.TEAM,
                "update id=" + teamId + ",color=" + updated.color());

        syncNativeTeam(currentServer, teamId);

        return updated;
    }

    public TeamDefinition update(Identifier teamId, UnaryOperator<TeamDefinition> updater) {
        return update(stableUuid(teamId.toString()), updater);
    }

    public TeamDefinition unregister(UUID teamId) {
        TEAM_MEMBERS.remove(teamId);
        TEAM_GAME.remove(teamId);

        MEMBER_TEAM.values().removeIf(id -> Objects.equals(id, teamId));

        // 🌟 從儲存庫刪除
        if (storage != null) {
            storage.delete(teamId);
        }

        TeamDefinition removed = TEAMS.remove(teamId);
        removeNativeTeam(currentServer, teamId);
        return removed;
    }

    public TeamDefinition unregister(Identifier teamId) {
        return unregister(stableUuid(teamId.toString()));
    }

    public List<TeamDefinition> unregisterGame(UUID gameId) {
        Objects.requireNonNull(gameId, "gameId");
        List<TeamDefinition> removed = new java.util.ArrayList<>();

        for (UUID teamId : new java.util.ArrayList<>(TEAMS.keySet())) {
            if (Objects.equals(TEAM_GAME.get(teamId), gameId)) {
                // 💡 這裡直接呼叫 unregister(teamId)，所以會自動觸發 storage.delete()，不需要額外寫邏輯
                TeamDefinition team = unregister(teamId);
                if (team != null) {
                    removed.add(team);
                }
            }
        }
        return List.copyOf(removed);
    }

    public List<TeamDefinition> unregisterGame(Identifier gameId) {
        return unregisterGame(stableUuid(gameId.toString()));
    }

    public TeamDefinition get(UUID teamId) {
        return TEAMS.get(teamId);
    }

    public TeamDefinition get(Identifier teamId) {
        return get(stableUuid(teamId.toString()));
    }

    public List<TeamDefinition> all(Identifier gameId) {
        Objects.requireNonNull(gameId, "gameId");
        return TEAMS.entrySet().stream()
                .filter(entry -> Objects.equals(TEAM_GAME.get(entry.getKey()), gameId))
                .map(Map.Entry::getValue)
                .toList();
    }

    public Map<UUID, TeamDefinition> snapshot(UUID gameId) {
        Objects.requireNonNull(gameId, "gameId");
        Map<UUID, TeamDefinition> snapshot = new LinkedHashMap<>();
        for (Map.Entry<UUID, TeamDefinition> entry : TEAMS.entrySet()) {
            if (Objects.equals(TEAM_GAME.get(entry.getKey()), gameId)) {
                snapshot.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(snapshot);
    }

    public boolean addPlayer(@NotNull UUID teamId, @NotNull UUID playerId) {
        if (!TEAMS.containsKey(teamId)) return false;

        TeamDefinition team = TEAMS.get(teamId);
        Set<UUID> teamMembers = TEAM_MEMBERS.computeIfAbsent(teamId, ignored -> new LinkedHashSet<>());
        if (!teamMembers.contains(playerId) && team.playerLimit() > 0 && teamMembers.size() >= team.playerLimit()) {
            return false;
        }

        UUID previous = MEMBER_TEAM.put(playerId, teamId);
        if (previous != null && !previous.equals(teamId)) {
            Set<UUID> previousMembers = TEAM_MEMBERS.get(previous);
            if (previousMembers != null) {
                previousMembers.remove(playerId);
            }
        }
        teamMembers.add(playerId);
        if (previous != null && !previous.equals(teamId)) {
            removeNativePlayerMember(currentServer, previous, playerId);
        }
        addNativePlayerMember(currentServer, teamId, playerId);
        DebugLogManager.INSTANCE.log(DebugFeature.TEAM,
                "add player=" + playerId + " -> team=" + teamId + (previous != null ? ",previous=" + previous : ""));
        return true;
    }

    public boolean addPlayer(Identifier teamId, @NotNull UUID playerId) {
        return addPlayer(stableUuid(teamId.toString()), playerId);
    }

    public boolean removePlayer(@NotNull UUID playerId) {
        UUID teamId = MEMBER_TEAM.remove(playerId);
        if (teamId == null) return false;

        Set<UUID> members = TEAM_MEMBERS.get(teamId);
        if (members != null) {
            members.remove(playerId);
        }
        removeNativePlayerMember(currentServer, teamId, playerId);
        DebugLogManager.INSTANCE.log(DebugFeature.TEAM, "remove player=" + playerId + " from team=" + teamId);
        return true;
    }

    public void forEachMember(UUID teamId, Consumer<UUID> action) {
        Objects.requireNonNull(teamId, "teamId");
        Objects.requireNonNull(action, "action");
        Set<UUID> members = TEAM_MEMBERS.get(teamId);
        if (members == null) {
            return;
        }
        for (UUID member : Set.copyOf(members)) {
            action.accept(member);
        }
    }

    public void forEachMember(Identifier teamId, Consumer<UUID> action) {
        forEachMember(stableUuid(teamId.toString()), action);
    }

    public UUID teamOf(UUID playerId) {
        return MEMBER_TEAM.get(playerId);
    }

    public Set<UUID> members(UUID teamId) {
        Set<UUID> members = TEAM_MEMBERS.get(teamId);
        return members == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(members));
    }

    public Set<UUID> members(Identifier teamId) {
        return members(stableUuid(teamId.toString()));
    }

    public List<TeamDefinition> all() {
        return List.copyOf(TEAMS.values());
    }

    public Map<UUID, TeamDefinition> snapshot() {
        return Map.copyOf(TEAMS);
    }

    public Map<UUID, UUID> teamGameIds() {
        return Map.copyOf(TEAM_GAME);
    }

    private static UUID stableUuid(String token) {
        return UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8));
    }

    public void clear() {
        if (currentServer != null) {
            for (UUID teamId : Set.copyOf(TEAMS.keySet())) {
                removeNativeTeam(currentServer, teamId);
            }
        }
        TEAMS.clear();
        TEAM_MEMBERS.clear();
        MEMBER_TEAM.clear();
        TEAM_GAME.clear();
    }

    private void syncAllNativeTeams(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (UUID teamId : TEAMS.keySet()) {
            syncNativeTeam(server, teamId);
        }
    }

    private void syncNativeTeam(MinecraftServer server, UUID teamId) {
        if (server == null || teamId == null) {
            return;
        }

        TeamDefinition definition = TEAMS.get(teamId);
        if (definition == null) {
            return;
        }

        Object scoreboard = server.getScoreboard();
        Object nativeTeam = getOrCreateNativeTeam(scoreboard, teamKey(teamId));
        if (nativeTeam == null) {
            return;
        }

        invokeIfPresent(nativeTeam, "setDisplayName", definition.translationKey());
        invokeIfPresent(nativeTeam, "setColor", definition.color().toChatFormatting());
        invokeIfPresent(nativeTeam, "setAllowFriendlyFire", flagOrDefault(definition, TeamFlag.FRIENDLY_FIRE, true));
        invokeIfPresent(nativeTeam, "setSeeFriendlyInvisibles", flagOrDefault(definition, TeamFlag.SEE_INVISIBLES, false));

        for (UUID memberId : TEAM_MEMBERS.getOrDefault(teamId, Set.of())) {
            String entry = resolveScoreboardEntry(server, memberId);
            invokeIfPresent(scoreboard, "addPlayerToTeam", entry, nativeTeam);
        }
    }

    private void removeNativeTeam(MinecraftServer server, UUID teamId) {
        if (server == null || teamId == null) {
            return;
        }
        Object scoreboard = server.getScoreboard();
        Object nativeTeam = invoke(scoreboard, "getPlayerTeam", teamKey(teamId));
        if (nativeTeam != null) {
            invokeIfPresent(scoreboard, "removePlayerTeam", nativeTeam);
        }
    }

    private void addNativePlayerMember(MinecraftServer server, UUID teamId, UUID playerId) {
        if (server == null || teamId == null || playerId == null) {
            return;
        }
        Object scoreboard = server.getScoreboard();
        Object nativeTeam = getOrCreateNativeTeam(scoreboard, teamKey(teamId));
        if (nativeTeam == null) {
            return;
        }
        invokeIfPresent(scoreboard, "addPlayerToTeam", resolveScoreboardEntry(server, playerId), nativeTeam);
    }

    private void removeNativePlayerMember(MinecraftServer server, UUID teamId, UUID playerId) {
        if (server == null || teamId == null || playerId == null) {
            return;
        }
        Object scoreboard = server.getScoreboard();
        Object nativeTeam = invoke(scoreboard, "getPlayerTeam", teamKey(teamId));
        if (nativeTeam == null) {
            return;
        }
        String entry = resolveScoreboardEntry(server, playerId);
        if (!invokeIfPresent(scoreboard, "removePlayerFromTeam", entry, nativeTeam)) {
            invokeIfPresent(scoreboard, "removePlayerFromTeam", entry);
        }
    }

    private Object getOrCreateNativeTeam(Object scoreboard, String teamKey) {
        Object nativeTeam = invoke(scoreboard, "getPlayerTeam", teamKey);
        if (nativeTeam != null) {
            return nativeTeam;
        }
        return invoke(scoreboard, "addPlayerTeam", teamKey);
    }

    private String resolveScoreboardEntry(MinecraftServer server, UUID playerId) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getScoreboardName();
        }
        return playerId.toString();
    }

    private boolean flagOrDefault(TeamDefinition definition, TeamFlag flag, boolean fallback) {
        Boolean value = definition.flags().get(flag);
        return value == null ? fallback : value;
    }

    private String teamKey(UUID id) {
        return id.toString();
    }

    private Object invoke(Object target, String method, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            for (var candidate : target.getClass().getMethods()) {
                if (!candidate.getName().equals(method) || candidate.getParameterCount() != args.length) {
                    continue;
                }
                if (isCompatible(candidate.getParameterTypes(), args)) {
                    return candidate.invoke(target, args);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private boolean invokeIfPresent(Object target, String method, Object... args) {
        if (target == null) {
            return false;
        }
        try {
            for (var candidate : target.getClass().getMethods()) {
                if (!candidate.getName().equals(method) || candidate.getParameterCount() != args.length) {
                    continue;
                }
                if (isCompatible(candidate.getParameterTypes(), args)) {
                    candidate.invoke(target, args);
                    return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private boolean isCompatible(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                if (parameterTypes[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> expected = wrapPrimitive(parameterTypes[i]);
            if (!expected.isInstance(arg)) {
                return false;
            }
        }
        return true;
    }

    private Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }



    public Set<UUID> getMembersByTeam(@Nullable UUID teamId) {
        if (teamId == null) return Set.of();
        else return TEAM_MEMBERS.getOrDefault(teamId, Set.of());
    }

    public @Nullable UUID getTeamByMember(@NotNull UUID memberId) {
        return MEMBER_TEAM.getOrDefault(memberId, null);
    }

    public int teamSize(@NotNull UUID teamId) {
        if (!hasTeam(teamId)) return -1;
        return TEAM_MEMBERS.getOrDefault(teamId, Set.of()).size();
    }


}