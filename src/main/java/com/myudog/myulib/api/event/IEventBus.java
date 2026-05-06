package com.myudog.myulib.api.event;

/**
 * 可實例化的事件匯流排介面。
 * 允許在不同的上下文（例如不同的遊戲房間、狀態機）中獨立宣告與使用。
 */
public interface IEventBus {

    <T extends IEvent> IEventListener<T> subscribe(Class<T> eventType, IEventListener<T> listener);

    <T extends IEvent> IEventListener<T> subscribe(Class<T> eventType, IEventListener<T> listener, EventPriority priority);

    <T extends IEvent> void unsubscribe(Class<T> eventType, IEventListener<T> listener);

    ProcessResult dispatch(IEvent event);

    void clear();
}