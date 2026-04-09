package com.myudog.myulib.api.game.state;

import com.myudog.myulib.api.game.state.GameState;
import com.myudog.myulib.api.game.bootstrap.GameBootstrapConfig;
import com.myudog.myulib.api.game.bootstrap.GameObjectConfig;
import com.myudog.myulib.api.game.components.ComponentModels.ComponentBindingDefinition;
import com.myudog.myulib.api.game.feature.GameFeature;
import com.myudog.myulib.api.game.logic.LogicContracts.LogicRule;
import com.myudog.myulib.api.game.logic.facts.LogicFactsResolver;
import com.myudog.myulib.api.identity.IdentityGroupDefinition;
import com.myudog.myulib.api.permission.PermissionSeed;
import net.minecraft.resources.Identifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class GameDefinition<S extends GameState> {
    private final Identifier id;
    private final S initialState;
    private final Map<S, Set<S>> allowedTransitions;
    public GameDefinition(Identifier id, S initialState, Map<S, Set<S>> allowedTransitions) {
        this.id = id;
        this.initialState = initialState;
        this.allowedTransitions = Map.copyOf(allowedTransitions);
    }
    public Identifier getId() { return id; }
    public S getInitialState() { return initialState; }
    public Map<S, Set<S>> getAllowedTransitions() { return allowedTransitions; }
    public Set<Identifier> getRequiredSpecialObjectIds() { return Collections.emptySet(); }
    public List<GameFeature> createFeatures(GameBootstrapConfig config) { return java.util.List.of(); }
    public List<IdentityGroupDefinition> createIdentityGroups(GameBootstrapConfig config) { return java.util.List.of(); }
    public List<PermissionSeed> createPermissionSeeds(GameBootstrapConfig config) { return java.util.List.of(); }
    public List<GameObjectConfig> createGameObjects(GameBootstrapConfig config) { return java.util.List.of(); }
    public List<LogicRule<S>> createLogicRules(GameBootstrapConfig config) { return java.util.List.of(); }
    public List<ComponentBindingDefinition> createComponentBindings(GameBootstrapConfig config) { return java.util.List.of(); }
    public LogicFactsResolver createLogicFactsResolver(GameBootstrapConfig config) { return LogicFactsResolver.DEFAULT; }
    public boolean isTransitionAllowed(S from, S to) {
        Set<S> allowed = allowedTransitions.get(from);
        return allowed != null && allowed.contains(to);
    }
}

