package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.game.object.IGameEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [D] 遊戲動態資料基底 (Game Data)
 * 負責存放遊戲進程中可變的資料。
 * 內建計時器、計分板與遊戲實體的集合，開發者可透過繼承 (例如 ChessData) 來擴充專屬資料。
 */
public abstract class GameData {

    // --- 計時器資料 ---
    private final Set<Integer> timerInstanceIds = new LinkedHashSet<>();
    private final Map<Integer, String> timerTags = new LinkedHashMap<>();

    // --- 計分板資料 ---
    private final List<String> scoreboardLines = new ArrayList<>();
    private final Map<String, Integer> scoreboardValues = new LinkedHashMap<>();

    // 🌟 修正：取代舊版 ObjectRuntime，用來追蹤遊戲內掛載的自訂生物/實體
    private final Set<IGameEntity> activeEntities = new LinkedHashSet<>();

    /**
     * 清理所有資料集合。
     * ⚠️ 注意：在呼叫此方法前，應確保 activeEntities 中的實體已從 Minecraft 世界中移除或卸載。
     */
    public void reset() {
        timerInstanceIds.clear();
        timerTags.clear();
        scoreboardLines.clear();
        scoreboardValues.clear();
        activeEntities.clear();
    }

    // --- Getters ---

    public final Set<Integer> timerInstanceIds() {
        return timerInstanceIds;
    }

    public final Map<Integer, String> timerTags() {
        return timerTags;
    }

    public final List<String> scoreboardLines() {
        return scoreboardLines;
    }

    public final Map<String, Integer> scoreboardValues() {
        return scoreboardValues;
    }

    // 🌟 獲取當前所有存活的遊戲實體
    public final Set<IGameEntity> activeEntities() {
        return activeEntities;
    }
}