package com.myudog.myulib.api.state;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StateTransitions<S extends IState<C>, C extends IStateContext<C>> {

    private final Map<S, Set<S>> allowedTransitions;

    public StateTransitions(Map<S, Set<S>> allowedTransitions) {
        // 複製 Map 確保外部無法竄改規則
        this.allowedTransitions = allowedTransitions == null ? Map.of() : Map.copyOf(allowedTransitions);
    }

    public boolean canTransition(S from, S to) {
        if (to == null) return false;

        // 允許自我切換
        if (Objects.equals(from, to)) return true;

        // 若未定義任何規則，視為完全開放 (自由切換)
        if (allowedTransitions.isEmpty()) return true;

        Set<S> allowed = allowedTransitions.get(from);
        return allowed != null && allowed.contains(to);
    }
}
