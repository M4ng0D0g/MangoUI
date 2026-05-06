package com.myudog.myulib.api.event;

import com.myudog.myulib.internal.event.ListenerRegistry;

public class EventBus implements IEventBus {

    private final IListenerRegistry registry = new ListenerRegistry();

    @Override
    public <T extends IEvent> IEventListener<T> subscribe(Class<T> eventType, IEventListener<T> listener) {
        return subscribe(eventType, listener, EventPriority.NORMAL);
    }

    @Override
    public <T extends IEvent> IEventListener<T> subscribe(Class<T> eventType, IEventListener<T> listener, EventPriority priority) {
        registry.register(eventType, listener, priority);
        return listener;
    }

    @Override
    public <T extends IEvent> void unsubscribe(Class<T> eventType, IEventListener<T> listener) {
        registry.unregister(eventType, listener);
    }

    @Override
    public ProcessResult dispatch(IEvent event) {
        final ProcessResult[] aggregated = { ProcessResult.PASS };

        registry.forEach(event.getClass(), listener -> {
            ProcessResult result = listener.handle(event);

            // 1. 條件式短路：遇到 CONSUME 或 FAILED，代表事件被吸收，停止迭代
            if (result == ProcessResult.CONSUME || result == ProcessResult.FAILED) {
                aggregated[0] = result;
                return false; // 回傳 false 讓 Registry 停止 forEach
            }

            // 2. 狀態聚合：遇到 SUCCESS，升級狀態但繼續交給下一位
            if (result == ProcessResult.SUCCESS) {
                aggregated[0] = ProcessResult.SUCCESS;
            }

            return true; // 繼續下一個監聽器
        });

        return aggregated[0];
    }

    @Override
    public void clear() {
        registry.clear();
    }
}