package com.myudog.myulib.api.state;

import org.jetbrains.annotations.NotNull;

public class StateMachine<S extends IState<C>, C extends IStateContext<C>> implements IStateMachine<S, C> {

    private final S initialState;
    private final StateTransitions<S, C> transitions;

    private S currentState;

    public StateMachine(@NotNull S initialState, @NotNull StateTransitions<S, C> transitions) {
        this.initialState = initialState;
        this.transitions = transitions;
        this.currentState = this.initialState;
    }

    @Override
    public S getCurrent() {
        return currentState;
    }

    @Override
    public boolean canTransition(S to) {
        return transitions.canTransition(currentState, to);
    }

    @Override
    public boolean transitionTo(@NotNull S to, C context) {
        if (!canTransition(to)) return false;
        if (currentState != null) currentState.onExit(context);
        currentState = to;
        currentState.onEnter(context);
        return true;
    }

    @Override
    public void forceTransition(@NotNull S to, C context) {
        if (currentState != null) currentState.onExit(context);
        currentState = to;
        currentState.onEnter(context);
    }

    @Override
    public void reset(C context) {
        if (currentState != null) currentState.onExit(context);
        currentState = initialState;
        currentState.onEnter(context);

    }

    @Override
    public void tick(C context) {
        if (currentState != null) currentState.onTick(context);
    }
}