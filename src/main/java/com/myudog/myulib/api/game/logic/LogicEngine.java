package com.myudog.myulib.api.game.logic;

import com.myudog.myulib.api.game.state.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogicEngine<S extends GameState> {
    private final List<LogicContracts.LogicRule<S>> rules = new ArrayList<>();

    public void register(LogicContracts.LogicRule<S> rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    public void registerAll(Iterable<LogicContracts.LogicRule<S>> rules) {
        if (rules == null) {
            return;
        }
        for (LogicContracts.LogicRule<S> rule : rules) {
            register(rule);
        }
    }

    public void clearRules() {
        rules.clear();
    }

    public List<LogicContracts.LogicRule<S>> rules() {
        return Collections.unmodifiableList(rules);
    }
}
