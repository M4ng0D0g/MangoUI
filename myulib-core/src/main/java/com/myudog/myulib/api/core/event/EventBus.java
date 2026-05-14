package com.myudog.myulib.api.core.event;

import com.myudog.myulib.internal.event.ListenerRegistry;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;

/**
 * EventBus
 *
 * 系統：核心事件系統 (Core Event System)
 * 角色：事件的中轉站，負責管理監聽器的註冊、註銷以及事件的派發與結果聚合。
 * 類型：Mediator / Dispatcher
 *
 * 此類別實作了 IEventBus 介面，並透過 ListenerRegistry 管理具備優先級的監聽器列表。
 */
public class EventBus implements IEventBus {

    private final IListenerRegistry registry = new ListenerRegistry();

    /**
     * 訂閱特定類型的事件，使用預設優先級 (NORMAL)。
     *
     * @param eventType 事件的類別
     * @param listener  處理事件的回呼邏輯
     * @param <T>       事件類型
     * @return 註冊成功的監聽器實例
     */
    @Override
    public <T extends IEvent> IEventListener<T> subscribe(Class<T> eventType, IEventListener<T> listener) {
        return subscribe(eventType, listener, EventPriority.NORMAL);
    }

    /**
     * 訂閱特定類型的事件，並指定優先級。
     *
     * @param eventType 事件的類別
     * @param listener  處理事件的回呼邏輯
     * @param priority  執行優先級 (HIGHEST 最先執行)
     * @param <T>       事件類型
     * @return 註冊成功的監聽器實例
     */
    @Override
    public <T extends IEvent> IEventListener<T> subscribe(Class<T> eventType, IEventListener<T> listener, EventPriority priority) {
        registry.register(eventType, listener, priority);
        DebugLogManager.INSTANCE.log(DebugFeature.EVENT,
                "subscribe event=" + (eventType == null ? "null" : eventType.getSimpleName()) + ",priority=" + priority);
        return listener;
    }

    @Override
    public <T extends IEvent> void unsubscribe(Class<T> eventType, IEventListener<T> listener) {
        registry.unregister(eventType, listener);
        DebugLogManager.INSTANCE.log(DebugFeature.EVENT,
                "unsubscribe event=" + (eventType == null ? "null" : eventType.getSimpleName()));
    }

    /**
     * 派發事件給所有已註冊的監聽器。
     * 派發過程會根據監聽器的處理結果進行狀態聚合與短路判斷。
     *
     * @param event 要派發的事件實例
     * @return 聚合後的處理結果 (ProcessResult)
     */
    @Override
    public ProcessResult dispatch(IEvent event) {
        final ProcessResult[] aggregated = { ProcessResult.PASS };

        registry.forEach(event.getClass(), listener -> {
            ProcessResult result = listener.handle(event);

            // 條件式短路：遇到 CONSUME (已消費) 或 FAILED (失敗)，代表事件被攔截，停止派發
            if (result == ProcessResult.CONSUME || result == ProcessResult.FAILED) {
                aggregated[0] = result;
                return false;
            }

            // 狀態聚合：遇到 SUCCESS (成功處理)，更新整體狀態但允許後續監聽器繼續處理
            if (result == ProcessResult.SUCCESS) {
                aggregated[0] = ProcessResult.SUCCESS;
            }

            return true;
        });

        DebugLogManager.INSTANCE.log(DebugFeature.EVENT,
            "dispatch event=" + event.getClass().getSimpleName() + ",result=" + aggregated[0]);

        return aggregated[0];
    }

    @Override
    public void clear() {
        registry.clear();
        DebugLogManager.INSTANCE.log(DebugFeature.EVENT, "clear listeners");
    }
}