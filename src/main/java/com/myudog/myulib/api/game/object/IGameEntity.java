package com.myudog.myulib.api.game.object;

import com.myudog.myulib.api.game.core.GameInstance;

/**
 * 自訂生物實作此介面，即可成為受遊戲管理的物件
 */
public interface IGameEntity {

    /**
     * 當遊戲系統生成這個實體時呼叫。
     * 實體應在此處向房間的 EventDispatcher 註冊自己需要的事件。
     */
    void attachToGame(GameInstance<?, ?, ?> instance);

    /**
     * 當遊戲結束或實體死亡時呼叫。
     * 實體務必在此處取消訂閱事件，以防 Memory Leak。
     */
    void detachFromGame(GameInstance<?, ?, ?> instance);
}