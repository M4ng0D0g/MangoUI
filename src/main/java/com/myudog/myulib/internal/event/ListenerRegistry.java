package com.myudog.myulib.internal.event;

import com.myudog.myulib.api.event.EventPriority;
import com.myudog.myulib.api.event.IEvent;
import com.myudog.myulib.api.event.IEventListener;
import com.myudog.myulib.api.event.IListenerRegistry;

import java.util.*;
import java.util.function.Predicate;

public class ListenerRegistry implements IListenerRegistry {

    // 私有化封裝：外部無法直接碰觸這個複雜結構
    private final Map<Class<? extends IEvent>, EnumMap<EventPriority, List<IEventListener<? extends IEvent>>>> listeners = new HashMap<>();

    @Override
    public <T extends IEvent> void register(Class<T> eventType, IEventListener<T> listener, EventPriority priority) {
        listeners.computeIfAbsent(eventType, k -> new EnumMap<>(EventPriority.class))
                .computeIfAbsent(priority, k -> new ArrayList<>())
                .add(listener);
    }

    @Override
    public <T extends IEvent> void unregister(Class<T> eventType, IEventListener<T> listener) {
        EnumMap<EventPriority, List<IEventListener<? extends IEvent>>> priorityMap = listeners.get(eventType);
        if (priorityMap == null) return;

        for (List<IEventListener<? extends IEvent>> list : priorityMap.values()) {
            if (list.remove(listener)) break;
        }
    }

    @Override
    public void forEach(Class<? extends IEvent> eventType, Predicate<IEventListener<IEvent>> action) {
        EnumMap<EventPriority, List<IEventListener<? extends IEvent>>> priorityMap = listeners.get(eventType);
        if (priorityMap == null || priorityMap.isEmpty()) return;

        // 遍歷優先級 (EnumMap 保證了 HIGHEST -> LOWEST 的順序)
        for (List<IEventListener<? extends IEvent>> list : priorityMap.values()) {
            if (list.isEmpty()) continue;

            // 建立快照，避免在執行時若監聽器進行反註冊導致崩潰
            List<IEventListener<? extends IEvent>> snapshot = new ArrayList<>(list);
            for (IEventListener<? extends IEvent> listener : snapshot) {
                @SuppressWarnings("unchecked")
                IEventListener<IEvent> typedListener = (IEventListener<IEvent>) listener;
                action.test(typedListener);
            }
        }
    }

    @Override
    public void clear() {
        listeners.clear();
    }
}