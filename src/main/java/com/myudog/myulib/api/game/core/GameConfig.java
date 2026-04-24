package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.game.object.IGameObject;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.team.TeamDefinition;
import com.myudog.myulib.api.team.TeamColor;
import com.myudog.myulib.api.team.TeamManager;
import com.myudog.myulib.api.timer.TimerDefinition;
import com.myudog.myulib.api.timer.TimerManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * [C] 遊戲設定藍圖 (Game Config)
 * 代表一局遊戲初始化所需的絕對靜態參數。
 * ⚠️ 必須保證為不可變 (Immutable)，且在遊戲實例建立後不得修改。
 */
public interface GameConfig {

    String SPECTATOR_TEAM_KEY = "spectator";
    Identifier SPECTATOR_TEAM_ID = Identifier.fromNamespaceAndPath("myulib", "spectator");


    /**
     * 驗證設定參數是否合法。
     * 若不合法，請拋出帶有具體錯誤訊息的 IllegalArgumentException，
     * 這樣外部的指令系統 (Command) 就能直接捕捉並回傳提示給玩家。
     *
     * @throws IllegalArgumentException 若參數不符合遊戲啟動條件
     */
    boolean validate();

    static boolean validateDefinitions(GameConfig config) {
        Objects.requireNonNull(config, "config");

        Map<Identifier, IGameObject> objects = Objects.requireNonNull(config.gameObjects(), "遊戲物件映射不得為空");
        if (objects.isEmpty()) throw new IllegalArgumentException("遊戲物件清單不得為空");

        if (config.maxPlayer() <= 0) {
            throw new IllegalArgumentException("maxPlayer 必須大於 0");
        }

        Map<String, Identifier> teamMap = Objects.requireNonNull(config.teams(), "teams 不得為空");
        Identifier spectatorTeam = teamMap.get(SPECTATOR_TEAM_KEY);
        if (spectatorTeam == null) {
            throw new IllegalArgumentException("teams 必須包含 spectator 隊伍");
        }
        if (!SPECTATOR_TEAM_ID.equals(spectatorTeam)) {
            throw new IllegalArgumentException("spectator 隊伍 id 必須是 " + SPECTATOR_TEAM_ID);
        }

        long playableTeamCount = teamMap.entrySet().stream()
                .filter(entry -> !SPECTATOR_TEAM_KEY.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        if (playableTeamCount < 1) {
            throw new IllegalArgumentException("teams 必須至少包含一個非 spectator 的可玩隊伍");
        }

        for (Map.Entry<String, Identifier> entry : teamMap.entrySet()) {
            String alias = entry.getKey();
            Identifier teamId = entry.getValue();
            if (alias == null || alias.isBlank()) {
                throw new IllegalArgumentException("teams 的 key 不得為空");
            }
            if (teamId == null) {
                throw new IllegalArgumentException("teams 的 team id 不得為空: " + alias);
            }
            if (!isScopedTokenIdentifier(teamId)) {
                throw new IllegalArgumentException("teams 的 team id 必須使用 token path 形式: " + teamId);
            }
        }

        for (Map.Entry<Identifier, IGameObject> entry : objects.entrySet()) {
            Identifier key = Objects.requireNonNull(entry.getKey(), "gameObjects key 不得為空");
            IGameObject template = Objects.requireNonNull(entry.getValue(), "gameObjects value 不得為空: " + key);

            if (!key.equals(template.getId())) {
                throw new IllegalArgumentException("gameObjects key 與物件 id 不一致: key=" + key + ", objectId=" + template.getId());
            }

            if (!template.validate()) {
                throw new IllegalArgumentException("遊戲物件模板驗證失敗: " + key);
            }

            IGameObject copy = Objects.requireNonNull(template.copy(), "遊戲物件 copy() 不得回傳 null: " + key);
            if (!copy.validate()) {
                throw new IllegalArgumentException("遊戲物件副本驗證失敗: " + key);
            }
        }

        for (FieldDefinition definition : config.fieldDefinitions()) {
            if (definition == null || !isScopedTokenIdentifier(definition.id())) {
                return false;
            }
            if (!FieldManager.INSTANCE.validate(definition)) {
                return false;
            }
        }
        for (RoleGroupDefinition definition : config.roleGroupDefinitions()) {
            if (definition == null || !isScopedTokenIdentifier(definition.id())) {
                return false;
            }
            if (!RoleGroupManager.INSTANCE.validate(definition)) {
                return false;
            }
        }
        for (TeamDefinition definition : config.teamDefinitions()) {
            if (definition == null || !isScopedTokenIdentifier(definition.id())) {
                return false;
            }
            if (!TeamManager.INSTANCE.validate(definition)) {
                return false;
            }
        }
        for (TimerDefinition definition : config.timerDefinitions()) {
            if (definition == null || !isScopedTokenIdentifier(definition.id)) {
                return false;
            }
            if (!TimerManager.INSTANCE.validate(definition)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 定義這局遊戲必須載入的遊戲物件藍圖 (例如：要在場上生成的自訂生物/棋子)。
     * 這些通常是在玩家輸入指令時，由指令層級預先定義好的。
     */
    Map<Identifier, IGameObject> gameObjects();

    default int maxPlayer() {
        return 16;
    }

    default boolean allowSpectating() {
        return false;
    }

    Map<String, Identifier> teams();

    default List<FieldDefinition> fieldDefinitions() {
        return List.of();
    }

    default List<RoleGroupDefinition> roleGroupDefinitions() {
        return List.of();
    }

    default List<TeamDefinition> teamDefinitions() {
        LinkedHashMap<Identifier, TeamDefinition> merged = new LinkedHashMap<>();
        merged.put(spectatorTeamDefinition().id(), spectatorTeamDefinition());
        for (TeamDefinition definition : additionalTeamDefinitions()) {
            if (definition != null) {
                merged.put(definition.id(), definition);
            }
        }
        return List.copyOf(merged.values());
    }

    List<TeamDefinition> additionalTeamDefinitions();

    default TeamDefinition spectatorTeamDefinition() {
        return new TeamDefinition(
                SPECTATOR_TEAM_ID,
                Component.literal("Spectator"),
                TeamColor.GRAY,
                Map.of(),
                0
        );
    }

    default List<TimerDefinition> timerDefinitions() {
        return List.of();
    }

    /**
     * 提供一個預設的空設定，適用於完全不需要外部參數的簡單遊戲。
     */
    static GameConfig empty() {
        return new GameConfig() {
            @Override
            public boolean validate() {
                return true;
            }

            @Override
            public Map<Identifier, IGameObject> gameObjects() {
                return Map.of();
            }

            @Override
            public Map<String, Identifier> teams() {
                return Map.of(
                        SPECTATOR_TEAM_KEY, SPECTATOR_TEAM_ID,
                        "default", Identifier.fromNamespaceAndPath("myulib", "default/default")
                );
            }

            @Override
            public List<TeamDefinition> additionalTeamDefinitions() {
                return List.of(new TeamDefinition(
                        Identifier.fromNamespaceAndPath("myulib", "default/default"),
                        Component.literal("Default"),
                        TeamColor.WHITE,
                        Map.of(),
                        0
                ));
            }
        };
    }

    private static boolean isScopedTokenIdentifier(Identifier identifier) {
        if (identifier == null) {
            return false;
        }
        String path = identifier.getPath();
        if (path == null || path.isBlank()) {
            return false;
        }
        String[] segments = path.split("/");
        return segments.length >= 2;
    }
}