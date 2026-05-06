package com.myudog.myulib.api.event;

@FunctionalInterface
public interface IEventListener<T extends IEvent> {
    ProcessResult handle(T event);
}
