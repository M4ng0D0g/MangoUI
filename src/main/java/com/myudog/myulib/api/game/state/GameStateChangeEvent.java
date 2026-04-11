package com.myudog.myulib.api.game.state;

import com.myudog.myulib.api.event.Event;
import com.myudog.myulib.api.game.core.GameInstance;

/**
 * 當遊戲狀態 (GameState) 發生改變時觸發的事件。
 * 取代了舊架構的 onEnter / onExit 生命週期，
 * 遊戲實體 (IGameEntity) 或系統可以透過訂閱此事件來執行對應邏輯。
 *
 * @param <S> 狀態列舉的型別 (例如 FightChessState)
 */
public record GameStateChangeEvent<S extends GameState>(
        GameInstance<?, ?, S> instance,
        S from,
        S to
) implements Event {

    /**
     * 輔助方法：判斷這是否為「剛進入」某個特定狀態的事件
     * (等同於舊版的 onEnter)
     */
    public boolean isEntering(S state) {
        return this.to == state;
    }

    /**
     * 輔助方法：判斷這是否為「剛離開」某個特定狀態的事件
     * (等同於舊版的 onExit)
     */
    public boolean isLeaving(S state) {
        return this.from == state;
    }
}