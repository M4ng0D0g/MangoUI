package com.myudog.myulib.api.game.core;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility for composing scoped identifiers with stable token structure:
 * namespace = mod token, path = game/definition/instance.
 */
public final class GameScopeTokens {
    private GameScopeTokens() {
    }

    public static Identifier scoped(String modToken, String gameToken, String definitionToken, String instanceToken) {
        return Identifier.fromNamespaceAndPath(
                normalize(modToken, "modToken"),
                String.join("/",
                        normalize(gameToken, "gameToken"),
                        normalize(definitionToken, "definitionToken"),
                        normalize(instanceToken, "instanceToken"))
        );
    }

    public static Identifier scoped(GameDefinition<?, ?, ?> definition, String definitionToken, String instanceToken) {
        Objects.requireNonNull(definition, "definition");
        return scoped(definition.modToken(), definition.gameToken(), definitionToken, instanceToken);
    }

    public static Identifier scopedFromParts(String modToken, String... tokens) {
        List<String> normalized = new ArrayList<>();
        if (tokens != null) {
            for (String token : tokens) {
                normalized.add(normalize(token, "token"));
            }
        }
        return Identifier.fromNamespaceAndPath(normalize(modToken, "modToken"), String.join("/", normalized));
    }

    private static String normalize(String token, String field) {
        String value = Objects.requireNonNull(token, field + " must not be null").trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}

