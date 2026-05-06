package com.myudog.myulib.api.event;

import java.util.function.Predicate;

public interface IListenerRegistry {

    /**
     * 註冊一個監聽器
     */
    <T extends IEvent> void register(Class<T> eventType, IEventListener<T> listener, EventPriority priority);

    /**
     * 移除一個監聽器
     */
    <T extends IEvent> void unregister(Class<T> eventType, IEventListener<T> listener);

    /**
     * 依照優先級順序，對所有符合該事件型別的監聽器執行特定操作
     * 使用 Consumer 模式可以避免直接暴露內部的 List 結構
     */
    void forEach(Class<? extends IEvent> eventType, Predicate<IEventListener<IEvent>> action);

    /**
     * 清空所有註冊資訊
     */
    void clear();
}