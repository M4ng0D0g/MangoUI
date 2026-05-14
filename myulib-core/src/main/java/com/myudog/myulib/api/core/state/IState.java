package com.myudog.myulib.api.core.state;

public interface IState<C extends IStateContext<C>> {
    // Enum 預設會實作 name()，所以實作者不需要自己寫
    String name();

    default void onEnter(C context) {
    }


    default void onExit(C context) {
    }
}