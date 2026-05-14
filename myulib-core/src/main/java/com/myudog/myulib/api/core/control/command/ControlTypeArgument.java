package com.myudog.myulib.api.core.control.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.myudog.myulib.api.core.control.ControlType;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ControlType 引數類型，用於指令中選擇 ControlType 枚舉。
 */
public class ControlTypeArgument implements ArgumentType<ControlType> {
    private static final DynamicCommandExceptionType INVALID_TYPE = new DynamicCommandExceptionType(
            (name) -> Component.literal("Invalid control type: " + name)
    );

    public static ControlTypeArgument type() {
        return new ControlTypeArgument();
    }

    public static ControlType getType(CommandContext<?> context, String name) {
        return context.getArgument(name, ControlType.class);
    }

    @Override
    public ControlType parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        try {
            return ControlType.parse(name);
        } catch (IllegalArgumentException e) {
            throw INVALID_TYPE.create(name);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (ControlType type : ControlType.values()) {
            String token = type.token();
            if (token.startsWith(remaining)) {
                builder.suggest(token);
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(ControlType.values()).map(ControlType::token).collect(Collectors.toList());
    }
}
