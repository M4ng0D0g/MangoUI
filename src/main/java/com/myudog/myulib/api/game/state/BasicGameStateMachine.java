package com.myudog.myulib.api.game.state;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BasicGameStateMachine<S extends GameState> implements GameStateMachine<S> {
    private final S initialState;
    private final Map<S, Set<S>> allowedTransitions;
    private S currentState;

    public BasicGameStateMachine(S initialState, Map<S, Set<S>> allowedTransitions) {
        this.initialState = Objects.requireNonNull(initialState, "Initial state 不能為空");
        // 複製 Map 確保外部無法竄改規則
        this.allowedTransitions = allowedTransitions == null ? Map.of() : Map.copyOf(allowedTransitions);
        this.currentState = this.initialState;
    }

    @Override
    public S getCurrent() {
        return currentState;
    }

    @Override
    public boolean canTransition(S to) {
        if (to == null) return false;

        // 允許自我切換
        if (Objects.equals(currentState, to)) return true;

        // 若未定義任何規則，視為完全開放 (自由切換)
        if (allowedTransitions.isEmpty()) return true;

        Set<S> allowed = allowedTransitions.get(currentState);
        return allowed != null && allowed.contains(to);
    }

    @Override
    public boolean transitionTo(S to) {
        if (!canTransition(to)) {
            return false;
        }
        currentState = to;
        return true;
    }

    @Override
    public void forceTransition(S to) {
        this.currentState = Objects.requireNonNull(to, "強制切換的目標狀態不能為 null");
    }

    @Override
    public void reset() {
        currentState = initialState;
    }
}