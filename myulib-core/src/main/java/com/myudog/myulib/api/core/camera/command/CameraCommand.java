package com.myudog.myulib.api.core.camera.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.myudog.myulib.api.core.animation.Easing;
import com.myudog.myulib.api.core.camera.CameraApi;
import com.myudog.myulib.api.core.camera.CameraTrackingTarget;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class CameraCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> camera = literal("camera")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(argument("players", EntityArgument.players())
                        .then(literal("clear")
                                .executes(CameraCommand::clear))
                        .then(literal("fade")
                                .then(literal("time")
                                        .then(argument("fadeIn", FloatArgumentType.floatArg(0, 10))
                                                .then(argument("hold", FloatArgumentType.floatArg(0, 10))
                                                        .then(argument("fadeOut", FloatArgumentType.floatArg(0, 10))
                                                                .then(literal("color")
                                                                        .then(argument("r", IntegerArgumentType.integer(0, 255))
                                                                                .then(argument("g", IntegerArgumentType.integer(0, 255))
                                                                                        .then(argument("b", IntegerArgumentType.integer(0, 255))
                                                                                                .executes(CameraCommand::fade))))))))))
                        .then(literal("fov_set")
                                .then(argument("fov", FloatArgumentType.floatArg(30, 110))
                                        .executes(ctx -> fovSet(ctx, 0, Easing.LINEAR))
                                        .then(argument("easeTime", FloatArgumentType.floatArg(0))
                                                .then(argument("easeType", StringArgumentType.word())
                                                        .executes(ctx -> fovSet(ctx, FloatArgumentType.getFloat(ctx, "easeTime"), parseEasing(StringArgumentType.getString(ctx, "easeType"))))))))
                        .then(literal("fov_clear")
                                .executes(ctx -> fovClear(ctx, 0, Easing.LINEAR))
                                .then(argument("easeTime", FloatArgumentType.floatArg(0))
                                        .then(argument("easeType", StringArgumentType.word())
                                                .executes(ctx -> fovClear(ctx, FloatArgumentType.getFloat(ctx, "easeTime"), parseEasing(StringArgumentType.getString(ctx, "easeType")))))))
                        .then(literal("set")
                                .then(argument("preset", StringArgumentType.word())
                                        .executes(CameraCommand::setPreset)
                                        .then(literal("ease")
                                                .then(argument("easeTime", FloatArgumentType.floatArg(0))
                                                        .then(argument("easeType", StringArgumentType.word())
                                                                .then(literal("pos")
                                                                        .then(argument("pos", Vec3Argument.vec3())
                                                                                .executes(CameraCommand::setPresetFull))))))))
                        .then(literal("attach_to_entity")
                                .then(argument("entity", EntityArgument.entity())
                                        .executes(CameraCommand::attachToEntity)))
                        .then(literal("detach_from_entity")
                                .executes(CameraCommand::detachFromEntity))
                );

        // Register under myulib literal
        dispatcher.register(literal("myulib").then(camera));
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        for (ServerPlayer player : players) {
            CameraApi.reset(player);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.myulib.camera.success.clear", players.size()), true);
        return players.size();
    }

    private static int fade(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        float in = FloatArgumentType.getFloat(context, "fadeIn");
        float hold = FloatArgumentType.getFloat(context, "hold");
        float out = FloatArgumentType.getFloat(context, "fadeOut");
        int r = IntegerArgumentType.getInteger(context, "r");
        int g = IntegerArgumentType.getInteger(context, "g");
        int b = IntegerArgumentType.getInteger(context, "b");
        for (ServerPlayer player : players) {
            CameraApi.fade(player, r, g, b, in, hold, out);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.myulib.camera.success.fade", players.size()), true);
        return players.size();
    }

    private static int fovSet(CommandContext<CommandSourceStack> context, float easeTime, Easing easing) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        float fov = FloatArgumentType.getFloat(context, "fov");
        for (ServerPlayer player : players) {
            CameraApi.setFov(player, fov, easeTime, easing);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.myulib.camera.success.fov", players.size()), true);
        return players.size();
    }

    private static int fovClear(CommandContext<CommandSourceStack> context, float easeTime, Easing easing) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        for (ServerPlayer player : players) {
            CameraApi.clearFov(player, easeTime, easing);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.myulib.camera.success.fov", players.size()), true);
        return players.size();
    }

    private static int setPreset(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        String preset = StringArgumentType.getString(context, "preset");
        for (ServerPlayer player : players) {
            CameraApi.setPreset(player, preset, CameraTrackingTarget.of(Vec3.ZERO), 0, Easing.LINEAR);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.myulib.camera.success.set", preset, players.size()), true);
        return players.size();
    }

    private static int setPresetFull(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        String preset = StringArgumentType.getString(context, "preset");
        float easeTime = FloatArgumentType.getFloat(context, "easeTime");
        Easing easing = parseEasing(StringArgumentType.getString(context, "easeType"));
        Vec3 pos = Vec3Argument.getVec3(context, "pos");
        for (ServerPlayer player : players) {
            CameraApi.setPreset(player, preset, CameraTrackingTarget.of(pos), (long)(easeTime * 1000), easing);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.myulib.camera.success.set", preset, players.size()), true);
        return players.size();
    }

    private static int attachToEntity(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        net.minecraft.world.entity.Entity entity = EntityArgument.getEntity(context, "entity");
        for (ServerPlayer player : players) {
            CameraApi.attachToEntity(player, entity);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.myulib.camera.success.attach", players.size()), true);
        return players.size();
    }

    private static int detachFromEntity(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        for (ServerPlayer player : players) {
            CameraApi.detachFromEntity(player);
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.myulib.camera.success.detach", players.size()), true);
        return players.size();
    }

    private static Easing parseEasing(String name) {
        try {
            // Convert Bedrock style in_out_quad to IN_OUT_QUAD
            return Easing.valueOf(name.toUpperCase().replace("-", "_"));
        } catch (Exception e) {
            return Easing.LINEAR;
        }
    }
}
