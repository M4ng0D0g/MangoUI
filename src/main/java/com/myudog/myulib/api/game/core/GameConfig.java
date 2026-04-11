package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.game.object.GameObjectConfig;

import java.util.List;
import java.util.Map;

/**
 * [C] 遊戲設定藍圖 (Game Config)
 * 代表一局遊戲初始化所需的絕對靜態參數。
 * ⚠️ 必須保證為不可變 (Immutable)，且在遊戲實例建立後不得修改。
 */
public interface GameConfig {

    /**
     * 驗證設定參數是否合法。
     * 若不合法，請拋出帶有具體錯誤訊息的 IllegalArgumentException，
     * 這樣外部的指令系統 (Command) 就能直接捕捉並回傳提示給玩家。
     *
     * @throws IllegalArgumentException 若參數不符合遊戲啟動條件
     */
    default void validate() throws IllegalArgumentException {
        // 預設為驗證通過，不拋出例外
    }

    /**
     * 定義這局遊戲必須載入的遊戲物件藍圖 (例如：要在場上生成的自訂生物/棋子)。
     * 這些通常是在玩家輸入指令時，由指令層級預先定義好的。
     */
    default List<GameObjectConfig<?>> gameObjects() {
        return List.of();
    }

    /**
     * 提供額外的動態字串參數，給不需要強型別的簡單設定使用。
     */
    default Map<String, String> metadata() {
        return Map.of();
    }

    /**
     * 提供一個預設的空設定，適用於完全不需要外部參數的簡單遊戲。
     */
    static GameConfig empty() {
        return new GameConfig() {};
    }
}