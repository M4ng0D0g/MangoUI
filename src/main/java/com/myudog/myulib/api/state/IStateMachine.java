package com.myudog.myulib.api.state;

public interface IStateMachine<S extends IState<C>, C extends IStateContext<C>> {

    S getCurrent();

    boolean canTransition(S to);
    boolean transitionTo(S to, C context);

    void forceTransition(S to, C context); // 強制切換狀態 (無視 canTransition 的規則限制)
    void reset(C context);

    void tick(C context);


}