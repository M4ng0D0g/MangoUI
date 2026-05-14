package com.myudog.myulib.api.framework.game.core;

import net.minecraft.resources.Identifier;
import java.util.*;

/**
 * GameConfig
 * 遊戲配置基類，支援動態變數註冊。
 */
public abstract class GameConfig {

    public final UUID SPECTATOR_TEAM = UUID.randomUUID();
    public final Identifier GAME_DEF_ID;
    
    private final Map<String, ConfigVariable<?>> variables = new LinkedHashMap<>();

    public GameConfig(Identifier gameDefId) {
        this.GAME_DEF_ID = gameDefId;
        registerVariables();
    }

    /**
     * 子類在此註冊可透過指令設定的變數。
     */
    protected abstract void registerVariables();

    protected <T> void register(String key, T defaultValue, java.util.function.Consumer<T> setter, java.util.function.Function<String, T> parser) {
        variables.put(key, new ConfigVariable<>(key, defaultValue, setter, parser));
    }

    public abstract boolean validate() throws Exception;

    public boolean allowSpectator() {
        return true;
    }

    public Map<String, ConfigVariable<?>> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    @SuppressWarnings("unchecked")
    public <T> void setVariable(String key, String value) {
        ConfigVariable<T> var = (ConfigVariable<T>) variables.get(key);
        if (var != null) {
            T parsed = var.parser.apply(value);
            var.setter.accept(parsed);
        }
    }

    public static record ConfigVariable<T>(
            String key,
            T defaultValue,
            java.util.function.Consumer<T> setter,
            java.util.function.Function<String, T> parser
    ) {}
}
