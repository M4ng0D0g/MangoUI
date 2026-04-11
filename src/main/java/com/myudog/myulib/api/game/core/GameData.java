package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.game.object.IGameEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GameData {
    private final Set<Integer> timerInstanceIds = new LinkedHashSet<>();
    private final Map<Integer, String> timerTags = new LinkedHashMap<>();

    // 🌟 修正：替換舊版 ObjectRuntime，改用 IGameEntity
    private final Set<IGameEntity> activeEntities = new LinkedHashSet<>();

    private final List<String> scoreboardLines = new ArrayList<>();
    private final Map<String, Integer> scoreboardValues = new LinkedHashMap<>();

    public void reset() {
        timerInstanceIds.clear();
        timerTags.clear();
        activeEntities.clear();
        scoreboardLines.clear();
        scoreboardValues.clear();
    }

    public final Set<Integer> timerInstanceIds() {
        return timerInstanceIds;
    }

    public final Map<Integer, String> timerTags() {
        return timerTags;
    }

    // 🌟 新增 Getter
    public final Set<IGameEntity> activeEntities() {
        return activeEntities;
    }

    public final List<String> scoreboardLines() {
        return scoreboardLines;
    }

    public final Map<String, Integer> scoreboardValues() {
        return scoreboardValues;
    }
}