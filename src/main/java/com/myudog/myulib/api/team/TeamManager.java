package com.myudog.myulib.api.team;

import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import com.myudog.myulib.api.util.ShortIdRegistry;
import com.myudog.myulib.api.storage.DataStorage;
import com.myudog.myulib.api.team.storage.NbtTeamStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class TeamManager {

    public static final TeamManager INSTANCE = new TeamManager();

    
    // 🌟 全面統一使用 Identifier 作為 Key
    private final Map<Identifier, TeamDefinition> TEAMS = new LinkedHashMap<>();
    private final Map<Identifier, Set<UUID>> TEAM_MEMBERS = new LinkedHashMap<>();
    private final Map<UUID, Identifier> PLAYER_TEAM = new LinkedHashMap<>();
    private final Map<Identifier, Identifier> TEAM_GAME_IDS = new LinkedHashMap<>();
    private final ShortIdRegistry ID_REGISTRY = new ShortIdRegistry(6);
    private MinecraftServer currentServer;

    // 🌟 儲存庫實例
    private DataStorage<Identifier, TeamDefinition> storage;

    private TeamManager() {
    }

    public void install() {
        install(new NbtTeamStorage());
    }

    /**
     * 🌟 依賴注入：在模組啟動時，傳入指定的儲存庫實作
     */
    public void install(DataStorage<Identifier, TeamDefinition> storageProvider) {
        storage = storageProvider;

        // 掛載伺服器啟動事件，初始化儲存庫並載入所有資料
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            currentServer = server;
            if (storage != null) {
                storage.initialize(server);

                // 將儲存庫中的資料載入記憶體
                Map<Identifier, TeamDefinition> loadedData = storage.loadAll();
                TEAMS.clear();
                ID_REGISTRY.clear();
                if (loadedData != null) {
                    TEAMS.putAll(loadedData);
                    for (Identifier id : loadedData.keySet()) {
                        ID_REGISTRY.generateAndBind(id);
                    }
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
            throw new IllegalArgumentException("TeamDefinition 驗證失敗: " + team.id());
        }
        TEAMS.put(team.id(), team);
        String shortId = ID_REGISTRY.generateAndBind(team.id());
        TEAM_MEMBERS.computeIfAbsent(team.id(), ignored -> new LinkedHashSet<>());
        TEAM_GAME_IDS.remove(team.id());

        // 🌟 寫入儲存庫
        if (storage != null) {
            storage.save(team.id(), team);
        }

        DebugLogManager.INSTANCE.log(DebugFeature.TEAM,
                "register id=" + team.id() + ",shortId=" + shortId + ",color=" + team.color());

        syncNativeTeam(currentServer, team.id());

        return team;
    }

    public boolean validate(TeamDefinition team) {
        return team != null && team.id() != null && team.playerLimit() >= 0 && !TEAMS.containsKey(team.id());
    }

    public TeamDefinition register(Identifier gameId, TeamDefinition team) {
        Objects.requireNonNull(gameId, "gameId");
        Objects.requireNonNull(team, "team");

        Identifier scopedId = scopedTeamId(gameId, team.id());
        if (TEAMS.containsKey(scopedId)) {
            throw new IllegalArgumentException("TeamDefinition scoped id 已存在: " + scopedId);
        }
        TeamDefinition scoped = new TeamDefinition(scopedId, team.translationKey(), team.color(), team.flags(), team.playerLimit());

        TEAMS.put(scoped.id(), scoped);
        String shortId = ID_REGISTRY.generateAndBind(scoped.id());
        TEAM_MEMBERS.computeIfAbsent(scoped.id(), ignored -> new LinkedHashSet<>());
        TEAM_GAME_IDS.put(scoped.id(), gameId);

        // 🌟 寫入儲存庫
        if (storage != null) {
            storage.save(scoped.id(), scoped);
        }

        DebugLogManager.INSTANCE.log(DebugFeature.TEAM,
                "register scoped id=" + scoped.id() + ",shortId=" + shortId + ",game=" + gameId + ",color=" + scoped.color());

        syncNativeTeam(currentServer, scoped.id());

        return scoped;
    }

    public TeamDefinition update(Identifier teamId, UnaryOperator<TeamDefinition> updater) {
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

    public TeamDefinition unregister(Identifier teamId) {
        TEAM_MEMBERS.remove(teamId);
        TEAM_GAME_IDS.remove(teamId);
        ID_REGISTRY.unbind(teamId);

        PLAYER_TEAM.values().removeIf(id -> Objects.equals(id, teamId));

        // 🌟 從儲存庫刪除
        if (storage != null) {
            storage.delete(teamId);
        }

        DebugLogManager.INSTANCE.log(DebugFeature.TEAM,
                "unregister id=" + teamId + ",shortId=" + ID_REGISTRY.getShortId(teamId));

        TeamDefinition removed = TEAMS.remove(teamId);
        removeNativeTeam(currentServer, teamId);
        return removed;
    }

    public List<TeamDefinition> unregisterGame(Identifier gameId) {
        Objects.requireNonNull(gameId, "gameId");
        List<TeamDefinition> removed = new java.util.ArrayList<>();

        for (Identifier teamId : new java.util.ArrayList<>(TEAMS.keySet())) {
            if (Objects.equals(teamGameIds.get(teamId), gameId)) {
                // 💡 這裡直接呼叫 unregister(teamId)，所以會自動觸發 storage.delete()，不需要額外寫邏輯
                TeamDefinition team = unregister(teamId);
                if (team != null) {
                    removed.add(team);
                }
            }
        }
        return List.copyOf(removed);
    }

    public TeamDefinition get(Identifier teamId) {
        return TEAMS.get(teamId);
    }

    public List<TeamDefinition> all(Identifier gameId) {
        Objects.requireNonNull(gameId, "gameId");
        return teams.entrySet().stream()
                .filter(entry -> Objects.equals(teamGameIds.get(entry.getKey()), gameId))
                .map(Map.Entry::getValue)
                .toList();
    }

    public static Map<Identifier, TeamDefinition> snapshot(Identifier gameId) {
        Objects.requireNonNull(gameId, "gameId");
        Map<Identifier, TeamDefinition> snapshot = new LinkedHashMap<>();
        for (Map.Entry<Identifier, TeamDefinition> entry : teams.entrySet()) {
            if (Objects.equals(teamGameIds.get(entry.getKey()), gameId)) {
                snapshot.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(snapshot);
    }

    public boolean addPlayer(Identifier teamId, UUID playerId) {
        if (!TEAMS.containsKey(teamId) || playerId == null) {
            return false;
        }
        TeamDefinition team = TEAMS.get(teamId);
        Set<UUID> teamMembers = TEAM_MEMBERS.computeIfAbsent(teamId, ignored -> new LinkedHashSet<>());
        if (!teamMembers.contains(playerId) && team.playerLimit() > 0 && teamMembers.size() >= team.playerLimit()) {
            return false;
        }

        Identifier previous = PLAYER_TEAM.put(playerId, teamId);
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

    public boolean removePlayer(UUID playerId) {
        Identifier teamId = PLAYER_TEAM.remove(playerId);
        if (teamId == null) {
            return false;
        }
        Set<UUID> members = TEAM_MEMBERS.get(teamId);
        if (members != null) {
            members.remove(playerId);
        }
        removeNativePlayerMember(currentServer, teamId, playerId);
        DebugLogManager.INSTANCE.log(DebugFeature.TEAM,
                "remove player=" + playerId + " from team=" + teamId);
        return true;
    }

    public void forEachMember(Identifier teamId, Consumer<UUID> action) {
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

    public Identifier teamOf(UUID playerId) {
        return PLAYER_TEAM.get(playerId);
    }

    public Set<UUID> members(Identifier teamId) {
        Set<UUID> members = TEAM_MEMBERS.get(teamId);
        return members == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(members));
    }

    public List<TeamDefinition> all() {
        return List.copyOf(TEAMS.values());
    }

    public static Map<Identifier, TeamDefinition> snapshot() {
        return Map.copyOf(teams);
    }

    public static Map<Identifier, Identifier> teamGameIds() {
        return Map.copyOf(teamGameIds);
    }

    public Identifier resolveShortId(String shortId) {
        return ID_REGISTRY.getFullId(shortId);
    }

    public String getShortIdOf(Identifier fullId) {
        return ID_REGISTRY.getShortId(fullId);
    }

    public Identifier scopedTeamId(@NotNull Identifier gameId, @NotNull Identifier teamId) {
        String prefix = gameId.getPath() + "_";

        if (teamId.getPath().startsWith(prefix) && teamId.getNamespace().equals(gameId.getNamespace())) {
            return teamId;
        }

        return Identifier.fromNamespaceAndPath(gameId.getNamespace(), prefix + teamId.getPath());
    }

    public void clear() {
        if (currentServer != null) {
            for (Identifier teamId : Set.copyOf(TEAMS.keySet())) {
                removeNativeTeam(currentServer, teamId);
            }
        }
        TEAMS.clear();
        TEAM_MEMBERS.clear();
        PLAYER_TEAM.clear();
        TEAM_GAME_IDS.clear();
        ID_REGISTRY.clear();
    }

    private void syncAllNativeTeams(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (Identifier teamId : TEAMS.keySet()) {
            syncNativeTeam(server, teamId);
        }
    }

    private void syncNativeTeam(MinecraftServer server, Identifier teamId) {
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

    private void removeNativeTeam(MinecraftServer server, Identifier teamId) {
        if (server == null || teamId == null) {
            return;
        }
        Object scoreboard = server.getScoreboard();
        Object nativeTeam = invoke(scoreboard, "getPlayerTeam", teamKey(teamId));
        if (nativeTeam != null) {
            invokeIfPresent(scoreboard, "removePlayerTeam", nativeTeam);
        }
    }

    private void addNativePlayerMember(MinecraftServer server, Identifier teamId, UUID playerId) {
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

    private void removeNativePlayerMember(MinecraftServer server, Identifier teamId, UUID playerId) {
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

    private String teamKey(Identifier id) {
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
}