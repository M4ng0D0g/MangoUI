package com.myudog.myulib.api.event;

public interface IFailableEvent extends IEvent {
    String getErrorMessage();

    void setErrorMessage(String errorMessage);
}
