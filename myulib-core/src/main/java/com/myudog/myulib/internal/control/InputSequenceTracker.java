package com.myudog.myulib.internal.control;

import com.myudog.myulib.api.core.control.Intent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家輸入序列追蹤器 (Input Sequence Tracker)
 * <p>
 * 負責紀錄每個玩家最近的輸入歷史，包含按下、放開、移動與時間戳記。
 * 供伺服器端開發者實現如「雙擊」、「連擊 (Combo)」或「特定手勢」偵測。
 */
public final class InputSequenceTracker {

    public static final InputSequenceTracker INSTANCE = new InputSequenceTracker();
    
    // 每個玩家最多保留的歷史紀錄長度
    private static final int MAX_HISTORY = 30;

    private final Map<UUID, Deque<Intent>> playerHistories = new ConcurrentHashMap<>();

    private InputSequenceTracker() {}

    /**
     * 追蹤一筆新的意圖
     */
    public void track(UUID playerId, Intent intent) {
        Deque<Intent> history = playerHistories.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        synchronized (history) {
            history.addFirst(intent);
            if (history.size() > MAX_HISTORY) {
                history.removeLast();
            }
        }
    }

    /**
     * 獲取玩家的輸入歷史（由新到舊）
     */
    public List<Intent> getHistory(UUID playerId) {
        Deque<Intent> history = playerHistories.get(playerId);
        if (history == null) return Collections.emptyList();
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    /**
     * 清理玩家歷史紀錄 (玩家斷線時呼叫)
     */
    public void clear(UUID playerId) {
        playerHistories.remove(playerId);
    }
    
    /**
     * 範例偵測方法：判斷是否為雙擊某個類型
     * @param playerId 玩家ID
     * @param type 意圖類型
     * @param thresholdMs 兩次點擊的最大間隔 (毫秒)
     */
    public boolean isDoubleClick(UUID playerId, com.myudog.myulib.api.core.control.IntentType type, long thresholdMs) {
        List<Intent> history = getHistory(playerId);
        int count = 0;
        long lastTime = -1;
        
        for (Intent intent : history) {
            if (intent.type() == type && intent.action() == com.myudog.myulib.api.core.control.InputAction.PRESS) {
                if (lastTime == -1) {
                    lastTime = intent.timestamp();
                    count++;
                } else {
                    long diff = lastTime - intent.timestamp();
                    return diff <= thresholdMs;
                }
            }
            if (count >= 2) break;
        }
        return false;
    }
}
