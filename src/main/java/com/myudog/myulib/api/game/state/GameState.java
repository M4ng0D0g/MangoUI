package com.myudog.myulib.api.game.state;

/**
 * [S] 遊戲狀態標記介面
 * 在輕量化架構中，通常由 Enum 實作此介面。
 * 生命週期的邏輯 (onEnter/onExit) 應透過 EventBus 在 GameDefinition 中綁定。
 */
public interface GameState {
    // Enum 預設會實作 name()，所以實作者不需要自己寫
    String name();
}