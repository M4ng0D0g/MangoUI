package com.myudog.myulib.api.core.control.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.myudog.myulib.api.core.control.ControlManager;
import com.myudog.myulib.api.core.control.ControlType;
import com.myudog.myulib.api.core.control.PlayerInputGate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;

import java.util.Set;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 控制系統指令介面 (/myulib control ...)
 */
public final class ControlCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("myulib")
                .then(literal("control")
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        // --- 控制綁定 (Binding) ---
                        .then(literal("bind")
                                .then(argument("source", EntityArgument.player())
                                        .then(argument("target", EntityArgument.entity())
                                                .executes(ControlCommand::bind))))
                        .then(literal("unbind")
                                .then(argument("source", EntityArgument.player())
                                        .then(argument("target", EntityArgument.entity())
                                                .executes(ControlCommand::unbind))))
                        .then(literal("unbind_sources")
                                .executes(ControlCommand::unbindSources))
                        .then(literal("unbind_targets")
                                .executes(ControlCommand::unbindTargets))
                        // --- 控制權限 (Permissions) ---
                        .then(literal("list")
                                .then(argument("player", EntityArgument.player())
                                        .executes(ControlCommand::list)))
                        .then(literal("allow")
                                .then(argument("player", EntityArgument.player())
                                        .then(argument("intent", ControlTypeArgument.type())
                                                .executes(ControlCommand::allow))))
                        .then(literal("deny")
                                .then(argument("player", EntityArgument.player())
                                        .then(argument("intent", ControlTypeArgument.type())
                                                .executes(ControlCommand::deny))))
                );

        dispatcher.register(command);
    }

    private static int bind(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer source = EntityArgument.getPlayer(context, "source");
        Entity target = EntityArgument.getEntity(context, "target");

        boolean success = ControlManager.INSTANCE.bind(source, target);
        if (success) {
            context.getSource().sendSuccess(() -> Component.literal("§aBound " + source.getName().getString() + " to " + target.getName().getString()), true);
        } else {
            context.getSource().sendFailure(Component.literal("Failed to bind. Target might be self or already bound."));
        }
        return 1;
    }

    private static int unbind(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer source = EntityArgument.getPlayer(context, "source");
        Entity target = EntityArgument.getEntity(context, "target");

        boolean success = ControlManager.INSTANCE.unbind(source, target.getUUID());
        if (success) {
            context.getSource().sendSuccess(() -> Component.literal("§eUnbound " + source.getName().getString() + " from " + target.getName().getString()), true);
        } else {
            context.getSource().sendFailure(Component.literal("No such binding found between these two."));
        }
        return 1;
    }

    private static int unbindSources(CommandContext<CommandSourceStack> context) {
        ControlManager.INSTANCE.clearAllBindings(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("§cGlobal: Unbound all sources and targets."), true);
        return 1;
    }

    private static int unbindTargets(CommandContext<CommandSourceStack> context) {
        // 在目前實作中，clearAllBindings 已經處理了雙向關係
        ControlManager.INSTANCE.clearAllBindings(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("§cGlobal: Unbound all sources and targets."), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Set<ControlType> denied = PlayerInputGate.INSTANCE.getDeniedTypes(player.getUUID());
        
        context.getSource().sendSuccess(() -> {
            String list = denied.isEmpty() ? "None" : denied.toString();
            return Component.literal("§bDenied intents for " + player.getName().getString() + ": §f" + list);
        }, false);
        return 1;
    }

    private static int allow(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ControlType type = ControlTypeArgument.getType(context, "intent");
        
        PlayerInputGate.INSTANCE.grant(player, type);
        context.getSource().sendSuccess(() -> Component.literal("§aAllowed " + type.token() + " for " + player.getName().getString()), true);
        return 1;
    }

    private static int deny(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ControlType type = ControlTypeArgument.getType(context, "intent");
        
        PlayerInputGate.INSTANCE.deny(player, type);
        context.getSource().sendSuccess(() -> Component.literal("§cDenied " + type.token() + " for " + player.getName().getString()), true);
        return 1;
    }
}
