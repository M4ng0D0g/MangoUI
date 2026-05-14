package com.myudog.myulib.api.core.command;

import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CommandRegistry {
    private static final Map<String, CommandAction<CommandContext, CommandResult>> COMMANDS = new LinkedHashMap<>();

    private CommandRegistry() {
    }

    public static void register(String name, CommandAction<CommandContext, CommandResult> action) {
        COMMANDS.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(action, "action"));
        DebugLogManager.INSTANCE.log(DebugFeature.COMMAND,
                "register: " + name);
    }

    public static CommandResult execute(CommandContext context) {
        CommandAction<CommandContext, CommandResult> action = COMMANDS.get(context.commandName());
        if (action == null) {
            DebugLogManager.INSTANCE.log(DebugFeature.COMMAND,
                    "execute unknown: " + context.commandName());
            return CommandResult.failure("Unknown command: " + context.commandName());
        }
        DebugLogManager.INSTANCE.log(DebugFeature.COMMAND,
                "execute: " + context.commandName());
        return action.execute(context);
    }

    public static Map<String, CommandAction<CommandContext, CommandResult>> snapshot() {
        return Map.copyOf(COMMANDS);
    }

    public static void clear() {
        COMMANDS.clear();
        DebugLogManager.INSTANCE.log(DebugFeature.COMMAND, "clear");
    }
}

