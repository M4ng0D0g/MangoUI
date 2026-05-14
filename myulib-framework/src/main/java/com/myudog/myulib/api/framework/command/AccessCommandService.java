package com.myudog.myulib.api.framework.command;

import com.myudog.myulib.Myulib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.myudog.myulib.api.core.control.ControlManager;
import com.myudog.myulib.api.core.control.ControlType;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import com.myudog.myulib.api.core.debug.DebugTraceManager;
import com.myudog.myulib.api.framework.field.FieldDefinition;
import com.myudog.myulib.api.framework.field.FieldVisualizationManager;
import com.myudog.myulib.api.framework.field.FieldManager;
import com.myudog.myulib.api.core.hologram.HologramDefinition;
import com.myudog.myulib.api.core.hologram.HologramManager;
import com.myudog.myulib.api.core.hologram.HologramFeature;
import com.myudog.myulib.api.core.hologram.network.HologramNetworking;
import com.myudog.myulib.api.framework.game.core.GameInstance;
import com.myudog.myulib.api.framework.game.core.GameManager;
import com.myudog.myulib.api.framework.permission.PermissionAction;
import com.myudog.myulib.api.framework.permission.PermissionDecision;
import com.myudog.myulib.api.framework.permission.PermissionManager;
import com.myudog.myulib.api.framework.permission.ScopeLayer;
import com.myudog.myulib.api.framework.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.framework.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.framework.team.TeamColor;
import com.myudog.myulib.api.framework.team.TeamDefinition;
import com.myudog.myulib.api.framework.team.TeamManager;
import com.myudog.myulib.api.core.timer.TimerDefinition;
import com.myudog.myulib.api.core.timer.TimerManager;
import com.myudog.myulib.api.core.command.CommandRegistry;
import com.myudog.myulib.api.core.command.CommandResult;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * AccessCommandService
 *
 * 蝟餌絞嚗?隞文??頂蝯?(Framework - Command)
 * 閫嚗?隞日脣暺?鞎痊撠??嗥?????踝?甈???隡?啜??蝑?????Minecraft 隡箸??冽?隞扎?
 * 憿?嚗ervice / Command Handler
 *
 * 甇斗??????憟?隞方酉???塚?
 * 1. ?折??{@link CommandRegistry}嚗?潭??嗅?函??摩?澆???????
 * 2. Minecraft ??Brigadier 蝟餌絞嚗?潭?靘策?拙振?恣?雿輻???脣?誘嚗?myulib ...嚗?
 */
public class AccessCommandService {
    public static final String COMMAND_PREFIX = Myulib.MOD_ID + ":";
    private static final Set<UUID> TRACKED_TIMERS = new LinkedHashSet<>();

    public static void registerDefaults() {
        registerLocalCommands();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerLegacyBaseCommand(dispatcher, Myulib.MOD_ID);
            registerFeatureCrudCommands(dispatcher);
        });
    }

    private static void registerLocalCommands() {
        CommandRegistry.register(COMMAND_PREFIX + "save", context -> {
            saveAll();
            return CommandResult.success("saved");
        });
        CommandRegistry.register(COMMAND_PREFIX + "status", context -> CommandResult.success(buildStatusLine()));

        registerLocalCrudMirror("field");
        registerLocalCrudMirror("permission");
        registerLocalCrudMirror("rolegroup");
        registerLocalCrudMirror("team");
        registerLocalCrudMirror("game");
        registerLocalCrudMirror("timer");
        registerLocalGameReadMirror();

        // Keep existing quick test mirrors.
        CommandRegistry.register(COMMAND_PREFIX + "field:count", context -> CommandResult.success("field=" + FieldManager.INSTANCE.all().size()));
        CommandRegistry.register(COMMAND_PREFIX + "permission:save", context -> {
            PermissionManager.INSTANCE.save();
            return CommandResult.success("permission_saved");
        });
        CommandRegistry.register(COMMAND_PREFIX + "permission:set-global", context -> {
            String group = normalizeGroupToken(context.arguments().getOrDefault("group", "everyone"));
            PermissionAction action = parseAction(context.arguments().getOrDefault("action", "BLOCK_BREAK"));
            PermissionDecision decision = parseDecision(context.arguments().getOrDefault("decision", "DENY"));
            grantGlobalPermission(group, action, decision);
            return CommandResult.success("permission=set:global," + group + "," + action + "=" + decision);
        });
        CommandRegistry.register(COMMAND_PREFIX + "rolegroup:count", context -> CommandResult.success("rolegroup=" + RoleGroupManager.INSTANCE.groups().size()));
        CommandRegistry.register(COMMAND_PREFIX + "team:count", context -> CommandResult.success("team=" + TeamManager.INSTANCE.all().size()));
        // game commands removed - GameManager API changed
        CommandRegistry.register(COMMAND_PREFIX + "timer:count", context -> CommandResult.success("timer=" + TimerManager.INSTANCE.timerDefinitionCount() + ",instance=" + TimerManager.INSTANCE.timerInstanceCount()));
        CommandRegistry.register(COMMAND_PREFIX + "camera:status", context -> CommandResult.success("camera_bridge_ready"));
        CommandRegistry.register(COMMAND_PREFIX + "control:status", context -> CommandResult.success("control_bindings=" + ControlManager.INSTANCE.controlledCount() + ",input_buffer=" + ControlManager.INSTANCE.bufferedInputCount()));
    }

    private static void registerLocalCrudMirror(String feature) {
        for (String op : List.of("create", "read", "update", "delete", "list")) {
            CommandRegistry.register(COMMAND_PREFIX + feature + ":" + op,
                    context -> CommandResult.success("ok:" + feature + ":" + op));
        }
    }

    private static void registerLocalGameReadMirror() {
        // Game instance commands disabled - needs reimplementation with new UUID-based API
        CommandRegistry.register(COMMAND_PREFIX + "game:read", context -> 
            CommandResult.failure("game_commands_not_yet_implemented")
        );
    }

    private static void registerLegacyBaseCommand(CommandDispatcher<CommandSourceStack> dispatcher, String rootLiteral) {
        dispatcher.register(Commands.literal(rootLiteral)
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("save").executes(context -> reply(context.getSource(), "[Myulib] saved: " + saveAll())))
                .then(Commands.literal("status").executes(context -> reply(context.getSource(), "[Myulib] " + buildStatusLine())))
        );
    }

    private static void registerFeatureCrudCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerFieldCrud(dispatcher);
        registerProjectionCrud(dispatcher);
        registerPermissionCrud(dispatcher);
        registerRoleGroupCrud(dispatcher);
        registerTeamCrud(dispatcher);
        registerGameCrud(dispatcher);
        registerTimerCrud(dispatcher);
        registerControlCrud(dispatcher);
        registerDebugCommands(dispatcher);
    }

    private static void registerControlCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND_PREFIX + "control")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("status")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    return reply(context.getSource(), renderControlStatus(target));
                                })
                                .then(Commands.literal("list")
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            return reply(context.getSource(), renderControlList(target));
                                        })
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(controlTypeSuggestions(), builder))
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    ControlType type;
                                                    try {
                                                        type = parseControlType(StringArgumentType.getString(context, "type"));
                                                    } catch (IllegalArgumentException ex) {
                                                        return reply(context.getSource(), "control=invalid_type");
                                                    }
                                                    return reply(context.getSource(), renderControlTypeStatus(target, type));
                                                })))
                                .then(Commands.argument("controltype", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(controlTypeSuggestions(), builder))
                                        .then(Commands.argument("state", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("enable", "disable"), builder))
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    ControlType type;
                                                    try {
                                                        type = parseControlType(StringArgumentType.getString(context, "controltype"));
                                                    } catch (IllegalArgumentException ex) {
                                                        return reply(context.getSource(), "control=invalid_type");
                                                    }

                                                    boolean enabled;
                                                    try {
                                                        enabled = parseControlState(StringArgumentType.getString(context, "state"));
                                                    } catch (IllegalArgumentException ex) {
                                                        return reply(context.getSource(), "control=invalid_state");
                                                    }

                                                    ControlManager.INSTANCE.setPlayerControl(target, type, enabled);
                                                    return reply(context.getSource(),
                                                            "control=set:" + target.getName().getString() + "," + type.name()
                                                                    + "=" + (enabled ? "enable" : "disable"));
                                                })))))
                .then(Commands.literal("bind")
                        .then(Commands.argument("playerFrom", EntityArgument.player())
                                .then(Commands.argument("entityTo", EntityArgument.entity())
                                        .executes(context -> {
                                            ServerPlayer from = EntityArgument.getPlayer(context, "playerFrom");
                                            Entity to = EntityArgument.getEntity(context, "entityTo");
                                            if (!ControlManager.INSTANCE.bind(from, to)) {
                                                return reply(context.getSource(), "control=bind_failed");
                                            }
                                            return reply(context.getSource(),
                                                    "control=bound:" + from.getName().getString() + "->" + to.getStringUUID());
                                        }))))
                .then(Commands.literal("unbind")
                        .then(Commands.literal("from")
                                .then(Commands.argument("playerFrom", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer from = EntityArgument.getPlayer(context, "playerFrom");
                                            ControlManager.INSTANCE.unbind(from);
                                            return reply(context.getSource(), "control=unbound_from:" + from.getName().getString());
                                        })))
                        .then(Commands.literal("to")
                                .then(Commands.argument("entityTo", EntityArgument.entity())
                                        .executes(context -> {
                                            Entity to = EntityArgument.getEntity(context, "entityTo");
                                            ControlManager.INSTANCE.unbindTo(to);
                                            return reply(context.getSource(), "control=unbound_to:" + to.getStringUUID());
                                        }))))
        );
    }

    private static void registerFieldCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND_PREFIX + "field")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)));

        root.then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("x1", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y1", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z1", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("x2", DoubleArgumentType.doubleArg())
                                                        .then(Commands.argument("y2", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("z2", DoubleArgumentType.doubleArg())
                                                                        .executes(context -> {
                                                                            String fieldToken = StringArgumentType.getString(context, "id");
                                                                            Identifier dimId = context.getSource().getLevel().dimension().identifier();
                                                                            double x1 = DoubleArgumentType.getDouble(context, "x1");
                                                                            double y1 = DoubleArgumentType.getDouble(context, "y1");
                                                                            double z1 = DoubleArgumentType.getDouble(context, "z1");
                                                                            double x2 = DoubleArgumentType.getDouble(context, "x2");
                                                                            double y2 = DoubleArgumentType.getDouble(context, "y2");
                                                                            double z2 = DoubleArgumentType.getDouble(context, "z2");
                                                                            AABB bounds = cuboidFromCorners(x1, y1, z1, x2, y2, z2);
                                                                            FieldDefinition field = new FieldDefinition(fieldToken, dimId, bounds, Map.of("source", "command"));
                                                                            createField(field);
                                                                            return reply(context.getSource(), "field=create:" + field.id());
                                                                        })))))))));

        root.then(Commands.literal("read")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(fieldIdSuggestions(), builder))
                        .executes(context -> {
                            String fieldToken = StringArgumentType.getString(context, "id");
                            UUID fieldId = resolveFieldIdToken(fieldToken);
                            FieldDefinition field = fieldId == null ? null : FieldManager.INSTANCE.get(fieldId);
                            if (field == null) {
                                return reply(context.getSource(), "field=not_found");
                            }
                            return reply(context.getSource(), "field=id:" + field.id() + ",dim:" + field.dimensionId());
                        })));

        root.then(Commands.literal("update")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(fieldIdSuggestions(), builder))
                        .executes(context -> {
                            String fieldToken = StringArgumentType.getString(context, "id");
                            UUID fieldId = resolveFieldIdToken(fieldToken);
                            if (fieldId == null) {
                                return reply(context.getSource(), "field=not_found");
                            }
                            FieldDefinition existing = FieldManager.INSTANCE.get(fieldId);
                            if (existing == null) {
                                return reply(context.getSource(), "field=not_found");
                            }
                            FieldManager.INSTANCE.unregister(fieldId);
                            var newData = new java.util.HashMap<String, Object>(existing.fieldData());
                            newData.put("updated", "true");
                            createField(new FieldDefinition(existing.token(), existing.dimensionId(), existing.bounds(), newData));
                            return reply(context.getSource(), "field=updated:" + fieldId);
                        })));

        root.then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(fieldIdSuggestions(), builder))
                        .executes(context -> {
                            String fieldToken = StringArgumentType.getString(context, "id");
                            UUID fieldId = resolveFieldIdToken(fieldToken);
                            if (fieldId == null || FieldManager.INSTANCE.get(fieldId) == null) {
                                return reply(context.getSource(), "field=not_found");
                            }
                            deleteField(fieldId);
                            return reply(context.getSource(), "field=deleted:" + fieldId);
                        })));

        root.then(Commands.literal("list")
                .executes(context -> reply(context.getSource(), "field=count:" + FieldManager.INSTANCE.all().size())));

        root.then(Commands.literal("visualize")
                .then(Commands.literal("on")
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return reply(context.getSource(), "field=visualize:player_only");
                            }
                            FieldVisualizationManager.INSTANCE.enable(context.getSource().getPlayer().getUUID());
                            return reply(context.getSource(), "field=visualize:on");
                        }))
                .then(Commands.literal("off")
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return reply(context.getSource(), "field=visualize:player_only");
                            }
                            var player = context.getSource().getPlayer();
                            FieldVisualizationManager.INSTANCE.disable(player.getUUID());
                            HologramNetworking.syncToPlayer(player, List.of());
                            return reply(context.getSource(), "field=visualize:off");
                        }))
                .then(Commands.literal("status")
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return reply(context.getSource(), "field=visualize:player_only");
                            }
                            var playerId = context.getSource().getPlayer().getUUID();
                            boolean enabled = FieldVisualizationManager.INSTANCE.isEnabled(playerId);
                            var style = FieldVisualizationManager.INSTANCE.getStyle(playerId);
                            return reply(context.getSource(), "field=visualize:" + (enabled ? "on" : "off")
                                    + ",radius=" + FieldVisualizationManager.INSTANCE.getRadius(playerId)
                                    + ",mode=" + FieldVisualizationManager.INSTANCE.getMode(playerId).token()
                                    + ",points=" + onOff(style.showPoints())
                                    + ",lines=" + onOff(style.showLines())
                                    + ",faces=" + onOff(style.showFaces())
                                    + ",name=" + onOff(style.showName())
                                    + ",axes=" + onOff(style.showAxes()));
                        }))
                .then(Commands.literal("radius")
                        .then(Commands.argument("value", IntegerArgumentType.integer(8, 256))
                                .executes(context -> {
                                    if (context.getSource().getPlayer() == null) {
                                        return reply(context.getSource(), "field=visualize:player_only");
                                    }
                                    var playerId = context.getSource().getPlayer().getUUID();
                                    int value = IntegerArgumentType.getInteger(context, "value");
                                    FieldVisualizationManager.INSTANCE.setRadius(playerId, value);
                                    return reply(context.getSource(), "field=visualize:radius=" + FieldVisualizationManager.INSTANCE.getRadius(playerId));
                                })))
                .then(Commands.literal("mode")
                        .then(Commands.argument("value", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(fieldModeSuggestions(), builder))
                                .executes(context -> {
                                    if (context.getSource().getPlayer() == null) {
                                        return reply(context.getSource(), "field=visualize:player_only");
                                    }
                                    var playerId = context.getSource().getPlayer().getUUID();
                                    String raw = StringArgumentType.getString(context, "value");
                                    FieldVisualizationManager.INSTANCE.setMode(playerId, FieldVisualizationManager.DisplayMode.parse(raw));
                                    return reply(context.getSource(), "field=visualize:mode=" + FieldVisualizationManager.INSTANCE.getMode(playerId).token());
                                })))
                .then(Commands.literal("show")
                        .then(Commands.argument("feature", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(HologramFeatureSuggestions(), builder))
                                .then(Commands.argument("enabled", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("on", "off"), builder))
                                        .executes(context -> {
                                            if (context.getSource().getPlayer() == null) {
                                                return reply(context.getSource(), "field=visualize:player_only");
                                            }
                                            var playerId = context.getSource().getPlayer().getUUID();
                                            HologramFeature feature = HologramFeature.parse(StringArgumentType.getString(context, "feature"));
                                            boolean enabled = "on".equalsIgnoreCase(StringArgumentType.getString(context, "enabled"));
                                            FieldVisualizationManager.INSTANCE.setFeature(playerId, feature, enabled);
                                            return reply(context.getSource(), "field=visualize:show:" + feature.token() + "=" + onOff(enabled));
                                        })))));

        dispatcher.register(root);
    }

    private static void registerDebugCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND_PREFIX + "debug")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)));

        root.then(Commands.literal("on").executes(context -> {
            if (context.getSource().getPlayer() == null) {
                return reply(context.getSource(), "debug=player_only");
            }
            var playerId = context.getSource().getPlayer().getUUID();
            DebugLogManager.INSTANCE.enable(playerId);
            return reply(context.getSource(), "debug=on");
        }));

        root.then(Commands.literal("off").executes(context -> {
            if (context.getSource().getPlayer() == null) {
                return reply(context.getSource(), "debug=player_only");
            }
            var playerId = context.getSource().getPlayer().getUUID();
            DebugLogManager.INSTANCE.disable(playerId);
            return reply(context.getSource(), "debug=off");
        }));

        root.then(Commands.literal("feature")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(debugFeatureSuggestions(), builder))
                        .then(Commands.argument("enabled", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("on", "off"), builder))
                                .executes(context -> {
                                    if (context.getSource().getPlayer() == null) {
                                        return reply(context.getSource(), "debug=player_only");
                                    }
                                    var playerId = context.getSource().getPlayer().getUUID();
                                    DebugFeature feature = DebugFeature.parse(StringArgumentType.getString(context, "name"));
                                    boolean enabled = "on".equalsIgnoreCase(StringArgumentType.getString(context, "enabled"));
                                    DebugLogManager.INSTANCE.setFeature(playerId, feature, enabled);
                                    return reply(context.getSource(), "debug=feature:" + feature.token() + "=" + (enabled ? "on" : "off"));
                                }))));

        root.then(Commands.literal("all")
                .then(Commands.argument("enabled", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("on", "off"), builder))
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return reply(context.getSource(), "debug=player_only");
                            }
                            var playerId = context.getSource().getPlayer().getUUID();
                            boolean enabled = "on".equalsIgnoreCase(StringArgumentType.getString(context, "enabled"));
                            DebugLogManager.INSTANCE.setAll(playerId, enabled);
                            return reply(context.getSource(), "debug=all=" + (enabled ? "on" : "off"));
                        })));

        root.then(Commands.literal("status").executes(context -> {
            if (context.getSource().getPlayer() == null) {
                return reply(context.getSource(), "debug=player_only");
            }
            var playerId = context.getSource().getPlayer().getUUID();
            String status = DebugLogManager.INSTANCE.isEnabled(playerId) ? "on" : "off";
            String features = DebugLogManager.INSTANCE.getFeatures(playerId).stream().map(DebugFeature::token).sorted().reduce((a, b) -> a + "|" + b).orElse("none");
            return reply(context.getSource(), "debug=status=" + status + "\nfeatures=" + features);
        }));

        root.then(Commands.literal("trace")
                .then(Commands.literal("on").executes(context -> {
                    if (context.getSource().getPlayer() == null) {
                        return reply(context.getSource(), "debug=player_only");
                    }
                    var playerId = context.getSource().getPlayer().getUUID();
                    DebugTraceManager.INSTANCE.enable(playerId);
                    return reply(context.getSource(), "debug=trace:on");
                }))
                .then(Commands.literal("off").executes(context -> {
                    if (context.getSource().getPlayer() == null) {
                        return reply(context.getSource(), "debug=player_only");
                    }
                    var playerId = context.getSource().getPlayer().getUUID();
                    DebugTraceManager.INSTANCE.disable(playerId);
                    return reply(context.getSource(), "debug=trace:off");
                }))
                .then(Commands.literal("status").executes(context -> {
                    if (context.getSource().getPlayer() == null) {
                        return reply(context.getSource(), "debug=player_only");
                    }
                    var playerId = context.getSource().getPlayer().getUUID();
                    return reply(context.getSource(), "debug=trace:" + (DebugTraceManager.INSTANCE.isEnabled(playerId) ? "on" : "off"));
                })));

        dispatcher.register(root);
    }

    private static void registerProjectionCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND_PREFIX + "hologram")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)));

        root.then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("x1", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y1", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z1", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("x2", DoubleArgumentType.doubleArg())
                                                        .then(Commands.argument("y2", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("z2", DoubleArgumentType.doubleArg())
                                                                        .executes(context -> {
                                                                            String hologramToken = StringArgumentType.getString(context, "id");
                                                                            UUID id = toScopedMyulibUUID(HologramDefinition.ROUTE, hologramToken);
                                                                            Identifier dimId = context.getSource().getLevel().dimension().identifier();
                                                                            double x1 = DoubleArgumentType.getDouble(context, "x1");
                                                                            double y1 = DoubleArgumentType.getDouble(context, "y1");
                                                                            double z1 = DoubleArgumentType.getDouble(context, "z1");
                                                                            double x2 = DoubleArgumentType.getDouble(context, "x2");
                                                                            double y2 = DoubleArgumentType.getDouble(context, "y2");
                                                                            double z2 = DoubleArgumentType.getDouble(context, "z2");
                                                                            AABB bounds = HologramManager.cuboidFromCorners(x1, y1, z1, x2, y2, z2);
                                                                            HologramManager.INSTANCE.register(new HologramDefinition(id, dimId, bounds, hologramToken));
                                                                            return reply(context.getSource(), "hologram=create:" + id);
                                                                        })))))))));

        root.then(Commands.literal("read")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(projectionIdSuggestions(), builder))
                        .executes(context -> {
                            String idToken = StringArgumentType.getString(context, "id");
                            UUID id = resolveProjectionIdToken(idToken);
                            HologramDefinition projection = HologramManager.INSTANCE.get(id);
                            if (projection == null) {
                                return reply(context.getSource(), "hologram=not_found");
                            }
                            return reply(context.getSource(), "hologram=id:" + projection.id() + ",dim:" + projection.dimensionId());
                        })));

        root.then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(projectionIdSuggestions(), builder))
                        .executes(context -> {
                            String idToken = StringArgumentType.getString(context, "id");
                            UUID id = resolveProjectionIdToken(idToken);
                            if (HologramManager.INSTANCE.get(id) == null) {
                                return reply(context.getSource(), "hologram=not_found");
                            }
                            HologramManager.INSTANCE.unregister(id);
                            return reply(context.getSource(), "hologram=deleted:" + id);
                        })));

        root.then(Commands.literal("list")
                .executes(context -> reply(context.getSource(), "hologram=count:" + HologramManager.INSTANCE.all().size())));

        root.then(Commands.literal("visualize")
                .then(Commands.literal("on")
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return reply(context.getSource(), "hologram=visualize:player_only");
                            }
                            FieldVisualizationManager.INSTANCE.enable(context.getSource().getPlayer().getUUID());
                            return reply(context.getSource(), "hologram=visualize:on");
                        }))
                .then(Commands.literal("off")
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return reply(context.getSource(), "hologram=visualize:player_only");
                            }
                            var player = context.getSource().getPlayer();
                            FieldVisualizationManager.INSTANCE.disable(player.getUUID());
                            HologramNetworking.syncToPlayer(player, List.of());
                            return reply(context.getSource(), "hologram=visualize:off");
                        }))
                .then(Commands.literal("status")
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return reply(context.getSource(), "hologram=visualize:player_only");
                            }
                            var playerId = context.getSource().getPlayer().getUUID();
                            var style = FieldVisualizationManager.INSTANCE.getStyle(playerId);
                            boolean enabled = FieldVisualizationManager.INSTANCE.isEnabled(playerId);
                            return reply(context.getSource(), "hologram=visualize:" + (enabled ? "on" : "off")
                                    + ",radius=" + FieldVisualizationManager.INSTANCE.getRadius(playerId)
                                    + ",points=" + onOff(style.showPoints())
                                    + ",lines=" + onOff(style.showLines())
                                    + ",faces=" + onOff(style.showFaces())
                                    + ",name=" + onOff(style.showName())
                                    + ",axes=" + onOff(style.showAxes()));
                        }))
                .then(Commands.literal("radius")
                        .then(Commands.argument("value", IntegerArgumentType.integer(8, 256))
                                .executes(context -> {
                                    if (context.getSource().getPlayer() == null) {
                                        return reply(context.getSource(), "hologram=visualize:player_only");
                                    }
                                    var playerId = context.getSource().getPlayer().getUUID();
                                    int value = IntegerArgumentType.getInteger(context, "value");
                                    FieldVisualizationManager.INSTANCE.setRadius(playerId, value);
                                    return reply(context.getSource(), "hologram=visualize:radius=" + FieldVisualizationManager.INSTANCE.getRadius(playerId));
                                })))
                .then(Commands.literal("show")
                        .then(Commands.argument("feature", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(HologramFeatureSuggestions(), builder))
                                .then(Commands.argument("enabled", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("on", "off"), builder))
                                        .executes(context -> {
                                            if (context.getSource().getPlayer() == null) {
                                                return reply(context.getSource(), "hologram=visualize:player_only");
                                            }
                                            var playerId = context.getSource().getPlayer().getUUID();
                                            HologramFeature feature = HologramFeature.parse(StringArgumentType.getString(context, "feature"));
                                            boolean enabled = "on".equalsIgnoreCase(StringArgumentType.getString(context, "enabled"));
                                            FieldVisualizationManager.INSTANCE.setFeature(playerId, feature, enabled);
                                            return reply(context.getSource(), "hologram=visualize:show:" + feature.token() + "=" + onOff(enabled));
                                        }))))
        );

        dispatcher.register(root);
    }

    private static void registerPermissionCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND_PREFIX + "permission")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)));

        root.then(Commands.literal("create")
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(permissionGroupSuggestions(), builder))
                        .then(permissionActionArgument()
                                .then(permissionDecisionArgument()
                                        .executes(context -> {
                                            String group = normalizeGroupToken(StringArgumentType.getString(context, "group"));
                                            PermissionAction action = parseAction(StringArgumentType.getString(context, "action"));
                                            PermissionDecision decision = parseDecision(StringArgumentType.getString(context, "decision"));
                                            grantGlobalPermission(group, action, decision);
                                            return reply(context.getSource(), "permission=create:global," + group + "," + action + "=" + decision);
                                        })))));

        root.then(Commands.literal("set")
                .then(Commands.literal("global")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(permissionGroupSuggestions(), builder))
                                .then(permissionActionArgument()
                                        .then(permissionDecisionArgument()
                                                .executes(context -> {
                                                    String group = normalizeGroupToken(StringArgumentType.getString(context, "group"));
                                                    PermissionAction action = parseAction(StringArgumentType.getString(context, "action"));
                                                    PermissionDecision decision = parseDecision(StringArgumentType.getString(context, "decision"));
                                                    setScopedGroupPermission(ScopeLayer.GLOBAL, Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "global"), group, action, decision);
                                                    return reply(context.getSource(), "permission=set:global," + group + "," + action + "=" + decision);
                                                })))))
                .then(Commands.argument("scope", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(scopeNamesForScopedSet(), builder))
                        .then(Commands.argument("scopeId", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(scopeIdSuggestions(StringArgumentType.getString(context, "scope")), builder))
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(permissionGroupSuggestions(), builder))
                                        .then(permissionActionArgument()
                                                .then(permissionDecisionArgument()
                                                        .executes(context -> {
                                                            ScopeLayer scope = parseScope(StringArgumentType.getString(context, "scope"));
                                                            Identifier scopeId = resolveScopeId(scope, StringArgumentType.getString(context, "scopeId"));
                                                            String group = normalizeGroupToken(StringArgumentType.getString(context, "group"));
                                                            PermissionAction action = parseAction(StringArgumentType.getString(context, "action"));
                                                            PermissionDecision decision = parseDecision(StringArgumentType.getString(context, "decision"));
                                                            setScopedGroupPermission(scope, scopeId, group, action, decision);
                                                            return reply(context.getSource(), "permission=set:" + scope.name().toLowerCase() + "," + group + "," + action + "=" + decision);
                                                        })))))));

        root.then(Commands.literal("read")
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(permissionGroupSuggestions(), builder))
                        .then(permissionActionArgument()
                                .executes(context -> {
                                    String group = normalizeGroupToken(StringArgumentType.getString(context, "group"));
                                    PermissionAction action = parseAction(StringArgumentType.getString(context, "action"));
                                    PermissionDecision decision = PermissionManager.INSTANCE.global().forGroup(group).get(action);
                                    return reply(context.getSource(), "permission=read:global," + group + "," + action + "=" + decision);
                                }))));

        root.then(Commands.literal("player")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            var player = EntityArgument.getPlayer(context, "player");
                            var groups = RoleGroupManager.INSTANCE.getSortedGroupIdsOf(player.getUUID());
                            Identifier dimId = player.level().dimension().identifier();
                            FieldDefinition field = FieldManager.INSTANCE.findAt(dimId, player.position()).orElse(null);
                            Identifier fieldId = field == null ? null : Identifier.fromNamespaceAndPath(Myulib.MOD_ID, FieldDefinition.ROUTE + "/" + field.id());
                            return reply(context.getSource(), renderFlattenedPlayerPermissions(player.getName().getString(), player.getUUID(), groups, fieldId, dimId));
                        })));

        root.then(Commands.literal("list")
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(permissionGroupSuggestions(), builder))
                        .executes(context -> {
                            String group = normalizeGroupToken(StringArgumentType.getString(context, "group"));
                            return reply(context.getSource(), renderPermissionGroupList(group, ScopeLayer.GLOBAL, null, "scope"));
                        })
                        .then(Commands.argument("scope", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(scopeNames(), builder))
                                .then(Commands.argument("scopeId", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(scopeIdSuggestions(StringArgumentType.getString(context, "scope")), builder))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("scope", "merged"), builder))
                                                .executes(context -> {
                                                    String group = normalizeGroupToken(StringArgumentType.getString(context, "group"));
                                                    ScopeLayer scope = parseScope(StringArgumentType.getString(context, "scope"));
                                                    Identifier scopeId = resolveScopeId(scope, StringArgumentType.getString(context, "scopeId"));
                                                    String mode = StringArgumentType.getString(context, "mode").toLowerCase();
                                                    return reply(context.getSource(), renderPermissionGroupList(group, scope, scopeId, mode));
                                                }))))));

        root.then(Commands.literal("delete")
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(permissionGroupSuggestions(), builder))
                        .then(permissionActionArgument()
                                .executes(context -> {
                                    String group = normalizeGroupToken(StringArgumentType.getString(context, "group"));
                                    PermissionAction action = parseAction(StringArgumentType.getString(context, "action"));
                                    grantGlobalPermission(group, action, PermissionDecision.UNSET);
                                    return reply(context.getSource(), "permission=deleted:global," + group + "," + action);
                                }))));

        dispatcher.register(root);
    }

    private static void registerRoleGroupCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND_PREFIX + "rolegroup")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("priority", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            String groupToken = StringArgumentType.getString(context, "id");
                                            UUID id = resolveRoleGroupIdToken(groupToken);
                                            int priority = IntegerArgumentType.getInteger(context, "priority");
                                            createRoleGroup(id, Component.literal(id.toString()), priority);
                                            return reply(context.getSource(), "rolegroup=create:" + id);
                                        }))))
                .then(Commands.literal("read")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(rolegroupIdSuggestions(), builder))
                                .executes(context -> {
                                    UUID id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
                                    RoleGroupDefinition group = RoleGroupManager.INSTANCE.get(id);
                                    if (group == null) {
                                        return reply(context.getSource(), "rolegroup=not_found");
                                    }
                                    return reply(context.getSource(), "rolegroup=id:" + group.id() + ",priority:" + group.priority());
                                })))
                .then(Commands.literal("update")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(rolegroupIdSuggestions(), builder))
                                .then(Commands.argument("priority", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            UUID id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
                                            if (RoleGroupManager.INSTANCE.get(id) == null) {
                                                return reply(context.getSource(), "rolegroup=not_found");
                                            }
                                            int priority = IntegerArgumentType.getInteger(context, "priority");
                                            RoleGroupManager.INSTANCE.update(id, old -> new RoleGroupDefinition(old.uuid(), old.translationKey(), priority, old.metadata(), old.members()));
                                            return reply(context.getSource(), "rolegroup=updated:" + id);
                                        }))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(rolegroupIdSuggestions(), builder))
                                .executes(context -> {
                                    UUID id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
                                    if (RoleGroupManager.INSTANCE.get(id) == null) {
                                        return reply(context.getSource(), "rolegroup=not_found");
                                    }
                                    deleteRoleGroup(id);
                                    return reply(context.getSource(), "rolegroup=deleted:" + id);
                                })))
                .then(Commands.literal("assign")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(rolegroupIdSuggestions(), builder))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            UUID id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
                                            if (RoleGroupManager.INSTANCE.get(id) == null) {
                                                return reply(context.getSource(), "rolegroup=not_found");
                                            }
                                            var target = EntityArgument.getPlayer(context, "player");
                                            boolean added = RoleGroupManager.INSTANCE.assign(target.getUUID(), id);
                                            return reply(context.getSource(), added
                                                    ? "rolegroup=assigned:" + id + "->" + target.getName().getString()
                                                    : "rolegroup=already_assigned");
                                        }))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(rolegroupIdSuggestions(), builder))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            UUID id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
                                            if (RoleGroupManager.INSTANCE.get(id) == null) {
                                                return reply(context.getSource(), "rolegroup=not_found");
                                            }
                                            var target = EntityArgument.getPlayer(context, "player");
                                            boolean removed = RoleGroupManager.INSTANCE.revoke(target.getUUID(), id);
                                            return reply(context.getSource(), removed
                                                    ? "rolegroup=revoked:" + id + "->" + target.getName().getString()
                                                    : "rolegroup=not_assigned");
                                        }))))
                .then(Commands.literal("groups-of")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    var target = EntityArgument.getPlayer(context, "player");
                                    List<String> groups = RoleGroupManager.INSTANCE.getSortedGroupIdsOf(target.getUUID());
                                    return reply(context.getSource(), "rolegroup=groups_of:" + target.getName().getString() + "=" + String.join("|", groups));
                                })))
                .then(Commands.literal("list")
                        .executes(context -> reply(context.getSource(), "rolegroup=count:" + RoleGroupManager.INSTANCE.groups().size()))));
    }

    private static void registerTeamCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND_PREFIX + "team")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .executes(context -> {
                                            String teamToken = StringArgumentType.getString(context, "id");
                                            Identifier id = toScopedMyulibIdentifier(TeamDefinition.ROUTE, teamToken);
                                            TeamColor color = parseTeamColor(StringArgumentType.getString(context, "color"));
                                            TeamManager.INSTANCE.register(new TeamDefinition(teamToken, Component.literal(id.toString()), color, Map.of()));
                                            return reply(context.getSource(), "team=create:" + id);
                                        }))))
                .then(Commands.literal("read")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(teamIdSuggestions(), builder))
                                .executes(context -> {
                                    UUID id = resolveTeamIdToken(StringArgumentType.getString(context, "id"));
                                    TeamDefinition team = TeamManager.INSTANCE.get(id);
                                    if (team == null) {
                                        return reply(context.getSource(), "team=not_found");
                                    }
                                    return reply(context.getSource(), "team=id:" + team.id() + ",color:" + team.color());
                                })))
                .then(Commands.literal("update")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(teamIdSuggestions(), builder))
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .executes(context -> {
                                            UUID id = resolveTeamIdToken(StringArgumentType.getString(context, "id"));
                                            if (TeamManager.INSTANCE.get(id) == null) {
                                                return reply(context.getSource(), "team=not_found");
                                            }
                                            TeamColor color = parseTeamColor(StringArgumentType.getString(context, "color"));
                                            TeamManager.INSTANCE.update(id, old -> new TeamDefinition(old.uuid().toString(), old.translationKey(), color, old.flags(), old.playerLimit()));
                                            return reply(context.getSource(), "team=updated:" + id);
                                         }))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(teamIdSuggestions(), builder))
                                .executes(context -> {
                                    UUID id = resolveTeamIdToken(StringArgumentType.getString(context, "id"));
                                    if (TeamManager.INSTANCE.get(id) == null) {
                                        return reply(context.getSource(), "team=not_found");
                                    }
                                    TeamManager.INSTANCE.unregister(id);
                                    return reply(context.getSource(), "team=deleted:" + id);
                                })))
                .then(Commands.literal("list")
                        .executes(context -> reply(context.getSource(), "team=count:" + TeamManager.INSTANCE.all().size()))));
    }

    private static void registerGameCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND_PREFIX + "game")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)));

        // /myulib game create <definition> <instanceId>
        root.then(Commands.literal("create")
                .then(Commands.argument("definition", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameDefinitionSuggestions(), builder))
                        .then(Commands.argument("instanceId", StringArgumentType.word())
                                .executes(context -> {
                                    Identifier defId = Identifier.parse(StringArgumentType.getString(context, "definition"));
                                    String instanceId = StringArgumentType.getString(context, "instanceId");
                                    GameManager.INSTANCE.createInstance(defId, instanceId, context.getSource().getLevel());
                                    return reply(context.getSource(), "game=created:" + instanceId);
                                }))));

        // /myulib game config <instanceId> <variable> <value>
        root.then(Commands.literal("config")
                .then(Commands.argument("instanceId", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                        .then(Commands.argument("variable", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String instanceId = StringArgumentType.getString(context, "instanceId");
                                    var instance = GameManager.INSTANCE.getInstance(instanceId);
                                    if (instance != null) {
                                        return SharedSuggestionProvider.suggest(instance.getConfig().getVariables().keySet(), builder);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String instanceId = StringArgumentType.getString(context, "instanceId");
                                            String variable = StringArgumentType.getString(context, "variable");
                                            String value = StringArgumentType.getString(context, "value");
                                            var instance = GameManager.INSTANCE.getInstance(instanceId);
                                            if (instance == null) return reply(context.getSource(), "game=not_found");
                                            instance.getConfig().setVariable(variable, value);
                                            return reply(context.getSource(), "game=config:" + instanceId + "," + variable + "=" + value);
                                        })))));

        // /myulib game init <instanceId>
        root.then(Commands.literal("init")
                .then(Commands.argument("instanceId", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                        .executes(context -> {
                            String instanceId = StringArgumentType.getString(context, "instanceId");
                            boolean success = GameManager.INSTANCE.initInstance(instanceId);
                            return reply(context.getSource(), success ? "game=initialized:" + instanceId : "game=init_failed");
                        })));

        // /myulib game join <player> <instanceId>
        root.then(Commands.literal("join")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("instanceId", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    String instanceId = StringArgumentType.getString(context, "instanceId");
                                    boolean success = GameManager.INSTANCE.joinPlayer(instanceId, player.getUUID(), null);
                                    return reply(context.getSource(), success ? "game=joined:" + instanceId : "game=join_failed");
                                }))));

        // /myulib game leave <player> <instanceId>
        root.then(Commands.literal("leave")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("instanceId", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    String instanceId = StringArgumentType.getString(context, "instanceId");
                                    GameManager.INSTANCE.leavePlayer(instanceId, player.getUUID());
                                    return reply(context.getSource(), "game=left:" + instanceId);
                                }))));

        // /myulib game leave_all <player>
        root.then(Commands.literal("leave_all")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            GameManager.INSTANCE.leaveAllInstances(player.getUUID());
                            return reply(context.getSource(), "game=left_all");
                        })));

        // /myulib game start <instanceId>
        root.then(Commands.literal("start")
                .then(Commands.argument("instanceId", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                        .executes(context -> {
                            String instanceId = StringArgumentType.getString(context, "instanceId");
                            boolean success = GameManager.INSTANCE.startInstance(instanceId);
                            return reply(context.getSource(), success ? "game=started:" + instanceId : "game=start_failed");
                        })));

        // /myulib game shutdown <instanceId>
        root.then(Commands.literal("shutdown")
                .then(Commands.argument("instanceId", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                        .executes(context -> {
                            String instanceId = StringArgumentType.getString(context, "instanceId");
                            boolean success = GameManager.INSTANCE.shutdownInstance(instanceId);
                            return reply(context.getSource(), success ? "game=shutdown:" + instanceId : "game=shutdown_failed");
                        })));

        // /myulib game delete <instanceId>
        root.then(Commands.literal("delete")
                .then(Commands.argument("instanceId", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                        .executes(context -> {
                            String instanceId = StringArgumentType.getString(context, "instanceId");
                            boolean success = GameManager.INSTANCE.deleteInstance(instanceId);
                            return reply(context.getSource(), success ? "game=deleted:" + instanceId : "game=delete_failed");
                        })));

        dispatcher.register(root);
    }
    
    private static void registerTimerCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND_PREFIX + "timer")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)));

        root.then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("ticks", LongArgumentType.longArg(1L))
                                .executes(context -> {
                                    String idToken = StringArgumentType.getString(context, "id");
                                    UUID id = resolveTimerIdToken(idToken);
                                    long ticks = LongArgumentType.getLong(context, "ticks");
                                    TimerManager.INSTANCE.register(new TimerDefinition(id, ticks));
                                    TRACKED_TIMERS.add(id);
                                    return reply(context.getSource(), "timer=create:" + id);
                                }))));

        root.then(Commands.literal("read")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(timerIdSuggestions(), builder))
                        .executes(context -> {
                            String idToken = StringArgumentType.getString(context, "id");
                            UUID id = resolveTimerIdToken(idToken);
                            return reply(context.getSource(), "timer=exists:" + TimerManager.INSTANCE.hasDefinition(id));
                        })));

        root.then(Commands.literal("update")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(timerIdSuggestions(), builder))
                        .then(Commands.argument("ticks", LongArgumentType.longArg(1L))
                                .executes(context -> {
                                    String idToken = StringArgumentType.getString(context, "id");
                                    UUID id = resolveTimerIdToken(idToken);
                                    long ticks = LongArgumentType.getLong(context, "ticks");
                                    if (!TimerManager.INSTANCE.hasDefinition(id)) {
                                        return reply(context.getSource(), "timer=not_found");
                                    }
                                    TimerManager.INSTANCE.unregister(id);
                                    TimerManager.INSTANCE.register(new TimerDefinition(id, ticks));
                                    TRACKED_TIMERS.add(id);
                                    return reply(context.getSource(), "timer=updated:" + id);
                                }))));

        root.then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(timerIdSuggestions(), builder))
                        .executes(context -> {
                            String idToken = StringArgumentType.getString(context, "id");
                            UUID id = resolveTimerIdToken(idToken);
                            boolean removed = TimerManager.INSTANCE.unregister(id) != null;
                            TRACKED_TIMERS.remove(id);
                            return reply(context.getSource(), removed ? "timer=deleted:" + id : "timer=not_found");
                        })));

        root.then(Commands.literal("list")
                .executes(context -> reply(context.getSource(), "timer=definition_count:" + TimerManager.INSTANCE.timerDefinitionCount() + ",tracked:" + TRACKED_TIMERS.size())));

        dispatcher.register(root);
    }

    private static PermissionAction parseAction(String raw) {
        return PermissionAction.valueOf(raw.toUpperCase());
    }

    private static PermissionDecision parseDecision(String raw) {
        return PermissionDecision.valueOf(raw.toUpperCase());
    }

    private static TeamColor parseTeamColor(String raw) {
        return TeamColor.valueOf(raw.toUpperCase());
    }

    private static ControlType parseControlType(String raw) {
        return ControlType.parse(raw);
    }

    private static boolean parseControlState(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Control state cannot be blank");
        }
        return switch (raw.trim().toLowerCase()) {
            case "enable", "enabled", "on", "true", "1" -> true;
            case "disable", "disabled", "off", "false", "0" -> false;
            default -> throw new IllegalArgumentException("Unknown control state: " + raw);
        };
    }

    private static UUID resolveFieldIdToken(String token) { return resolveStableUuidToken(token); }

    private static void ensureTicTacToeBlueTeamExists() {
        // no-op: retained for compatibility with older command flows
    }

    private static UUID resolveRoleGroupIdToken(String token) { return resolveStableUuidToken(token); }

    private static UUID resolveTeamIdToken(String token) { return resolveStableUuidToken(token); }

    private static UUID resolveTimerIdToken(String token) { return resolveStableUuidToken(token); }

    private static String normalizeGameInstanceName(String token) {
        return token == null ? "" : token.trim().toLowerCase();
    }

    private static String saveAll() {
        PermissionManager.INSTANCE.save();
        FieldManager.INSTANCE.save();
        RoleGroupManager.INSTANCE.save();
        return "permission,field,rolegroup";
    }

    private static String buildStatusLine() {
        return "field=" + FieldManager.INSTANCE.all().size()
                + ",rolegroup=" + RoleGroupManager.INSTANCE.groups().size()
                + ",team=" + TeamManager.INSTANCE.all().size()
                + ",game_instance=" + GameManager.INSTANCE.getInstances().size()
                + ",timer=" + TimerManager.INSTANCE.timerDefinitionCount() + ",timer_instance=" + TimerManager.INSTANCE.timerInstanceCount()
                + ",control_bindings=" + ControlManager.INSTANCE.controlledCount();
    }

    private static int reply(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), true);
        DebugLogManager.INSTANCE.log(DebugFeature.COMMAND,
                "source=" + source.getTextName() + ",message=" + message.replace('\n', '|'));
        return 1;
    }

    // --- Service Layer helpers ---
    public static void createRoleGroup(UUID groupId, MutableComponent displayName, int priority) {
        UUID id = groupId == null ? resolveStableUuidToken(null) : groupId;
        RoleGroupDefinition group = new RoleGroupDefinition(id, displayName, priority, Map.of(), Set.of());
        RoleGroupManager.INSTANCE.register(group);
        RoleGroupManager.INSTANCE.save();
    }

    public static void deleteRoleGroup(UUID groupId) {
        RoleGroupManager.INSTANCE.delete(groupId);
        RoleGroupManager.INSTANCE.save();
    }

    public static List<RoleGroupDefinition> listRoleGroups() {
        return RoleGroupManager.INSTANCE.groups();
    }

    public static void grantGlobalPermission(String groupId, PermissionAction action, PermissionDecision decision) {
        String normalized = PermissionManager.INSTANCE.normalizeGroupName(groupId);
        PermissionManager.INSTANCE.global().forGroup(normalized).set(action, decision);
        PermissionManager.INSTANCE.save();
    }

    public static void createField(FieldDefinition field) {
        FieldManager.INSTANCE.register(field);
        FieldManager.INSTANCE.save();
    }

    public static void deleteField(UUID fieldId) {
        FieldManager.INSTANCE.unregister(fieldId);
        FieldManager.INSTANCE.save();
    }

    private static List<String> permissionActionNames() {
        return java.util.Arrays.stream(PermissionAction.values()).map(Enum::name).toList();
    }

    private static List<String> permissionDecisionNames() {
        return java.util.Arrays.stream(PermissionDecision.values()).map(Enum::name).toList();
    }

    private static List<String> controlTypeSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (ControlType type : ControlType.values()) {
            suggestions.add(type.token());
            suggestions.add("player_" + type.token());
            suggestions.add(type.name());
            suggestions.add("PLAYER_" + type.name());
        }
        return List.copyOf(suggestions);
    }

    private static String renderControlStatus(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Set<ControlType> disabled = ControlManager.INSTANCE.effectiveDisabledPlayerControls(playerId);
        String target = ControlManager.INSTANCE.targetOfController(playerId).map(UUID::toString).orElse("none");
        String controller = ControlManager.INSTANCE.controllerOfTarget(playerId).map(UUID::toString).orElse("none");

        return "control=status"
                + "\nplayer=" + player.getName().getString()
                + "\nid=" + playerId
                + "\ndisabled=" + (disabled.isEmpty() ? "none" : disabled.stream().map(Enum::name).sorted().reduce((a, b) -> a + "|" + b).orElse("none"))
                + "\ncontrolling=" + target
                + "\ncontrolled_by=" + controller;
    }

    private static String renderControlList(ServerPlayer player) {
        StringBuilder builder = new StringBuilder();
        builder.append("control=list")
                .append("\nplayer=").append(player.getName().getString());
        UUID playerId = player.getUUID();
        for (ControlType type : ControlType.values()) {
            boolean enabled = ControlManager.INSTANCE.isPlayerControlEnabled(playerId, type);
            builder.append("\n").append(type.name()).append("=").append(enabled ? "enable" : "disable");
        }
        return builder.toString();
    }

    private static String renderControlTypeStatus(ServerPlayer player, ControlType type) {
        boolean enabled = ControlManager.INSTANCE.isPlayerControlEnabled(player.getUUID(), type);
        return "control=list"
                + "\nplayer=" + player.getName().getString()
                + "\n" + type.name() + "=" + (enabled ? "enable" : "disable");
    }

    private static ScopeLayer parseScope(String raw) {
        return ScopeLayer.valueOf(raw.toUpperCase());
    }

    private static Identifier resolveScopeId(ScopeLayer scope, String token) {
        return switch (scope) {
            case FIELD -> {
                Identifier fieldId = PermissionManager.INSTANCE.resolveFieldShortId(token);
                yield fieldId != null ? fieldId : toScopedMyulibIdentifier(FieldDefinition.ROUTE, token);
            }
            case DIMENSION -> {
                Identifier dimId = PermissionManager.INSTANCE.resolveDimensionShortId(token);
                yield dimId != null ? dimId : Identifier.parse(token);
            }
            case GLOBAL -> Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "global");
            case USER -> Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "user");
        };
    }

    private static void setScopedGroupPermission(ScopeLayer scope, Identifier scopeId, String group, PermissionAction action, PermissionDecision decision) {
        String normalized = PermissionManager.INSTANCE.normalizeGroupName(group);
        switch (scope) {
            case GLOBAL -> PermissionManager.INSTANCE.global().forGroup(normalized).set(action, decision);
            case DIMENSION -> PermissionManager.INSTANCE.dimension(scopeId).forGroup(normalized).set(action, decision);
            case FIELD -> PermissionManager.INSTANCE.field(scopeId).forGroup(normalized).set(action, decision);
        }
        PermissionManager.INSTANCE.save();
    }

    private static String renderPermissionGroupList(String group, ScopeLayer scope, Identifier scopeId, String mode) {
        String normalized = PermissionManager.INSTANCE.normalizeGroupName(group);
        StringBuilder builder = new StringBuilder();
        builder.append("permission=list\n");
        builder.append("group=").append(normalized)
                .append("\nscope=").append(scope.name().toLowerCase())
                .append("\nid=").append(scopeId == null ? "-" : scopeId.toString())
                .append("\nmode=").append(mode)
                .append("\n");
        for (PermissionAction action : PermissionAction.values()) {
            PermissionDecision decision;
            if ("merged".equalsIgnoreCase(mode)) {
                Identifier fieldId = scope == ScopeLayer.FIELD ? scopeId : null;
                Identifier dimId = scope == ScopeLayer.FIELD ? fieldDimensionOf(scopeId) : (scope == ScopeLayer.DIMENSION ? scopeId : null);
                decision = PermissionManager.INSTANCE.resolveGroupMerged(normalized, action, fieldId, dimId);
            } else {
                decision = PermissionManager.INSTANCE.resolveGroupInScope(normalized, action, scope, scopeId);
            }
            builder.append(action.name()).append("=").append(decision.name()).append("\n");
        }
        return builder.toString().trim();
    }

    private static Identifier fieldDimensionOf(Identifier fieldId) {
        com.myudog.myulib.api.framework.field.FieldDefinition field = fieldId == null ? null : FieldManager.INSTANCE.get(fieldId);
        return field == null ? null : field.dimensionId();
    }

    private static String normalizeGroupToken(String token) {
        return PermissionManager.INSTANCE.normalizeGroupName(token);
    }

    private static AABB cuboidFromCorners(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AABB(
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.min(z1, z2),
                Math.max(x1, x2),
                Math.max(y1, y2),
                Math.max(z1, z2)
        );
    }

    private static List<String> scopeNames() {
        return List.of("global", "dimension", "field");
    }

    private static List<String> scopeNamesForScopedSet() {
        return List.of("dimension", "field");
    }

    private static List<String> fieldModeSuggestions() {
        return List.of("edges-only", "full", "labels-only");
    }

    private static List<String> HologramFeatureSuggestions() {
        return java.util.Arrays.stream(HologramFeature.values()).map(HologramFeature::token).toList();
    }

    private static List<String> projectionIdSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (HologramDefinition hologram : HologramManager.INSTANCE.all().values()) {
            suggestions.add(hologram.token().toString());
        }
        return List.copyOf(suggestions);
    }

    private static UUID resolveProjectionIdToken(String token) {
        return resolveStableUuidToken(token);
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> permissionActionArgument() {
        return Commands.argument("action", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(permissionActionNames(), builder));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> permissionDecisionArgument() {
        return Commands.argument("decision", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(permissionDecisionNames(), builder));
    }

    private static List<String> rolegroupIdSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (RoleGroupDefinition group : RoleGroupManager.INSTANCE.groups()) {
            suggestions.add(group.token().toString());
        }
        return List.copyOf(suggestions);
    }

    private static List<String> fieldIdSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (FieldDefinition field : FieldManager.INSTANCE.all().values()) {
            suggestions.add(field.token().toString());
        }
        return List.copyOf(suggestions);
    }

    private static List<String> debugFeatureSuggestions() {
        return java.util.Arrays.stream(DebugFeature.values()).map(DebugFeature::token).toList();
    }

    private static List<String> permissionGroupSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        suggestions.add("everyone");

        for (RoleGroupDefinition group : RoleGroupManager.INSTANCE.groups()) {
            suggestions.add(group.token().toString());
            suggestions.add(PermissionManager.INSTANCE.normalizeGroupName(group.token().toString()));
        }

        for (String known : PermissionManager.INSTANCE.knownGroupNames()) {
            if (known == null || known.isBlank()) {
                continue;
            }
            if (known.contains(":")) {
                try {
                    suggestions.add(Identifier.parse(known).getPath());
                } catch (Exception ignored) {
                    suggestions.add(known);
                }
                continue;
            }
            suggestions.add(known);
        }
        return List.copyOf(suggestions);
    }

    private static List<String> scopeIdSuggestions(String scopeRaw) {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        ScopeLayer scope;
        try {
            scope = parseScope(scopeRaw);
        } catch (Exception ignored) {
            return List.of();
        }

        if (scope == ScopeLayer.DIMENSION) {
            for (FieldDefinition field : FieldManager.INSTANCE.all().values()) {
                suggestions.add(field.dimensionId().getPath());
            }
            for (Identifier id : PermissionManager.INSTANCE.dimensionScopeIds()) {
                suggestions.add(id.getPath());
            }
        } else if (scope == ScopeLayer.FIELD) {
            for (FieldDefinition field : FieldManager.INSTANCE.all().values()) {
                suggestions.add(field.token().toString());
            }
            for (Identifier id : PermissionManager.INSTANCE.fieldScopeIds()) {
                suggestions.add(id.getPath());
            }
        } else if (scope == ScopeLayer.GLOBAL) {
            suggestions.add("global");
        }

        return List.copyOf(suggestions);
    }

    private static List<String> timerIdSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (UUID id : TRACKED_TIMERS) {
            suggestions.add(id.toString());
        }
        return List.copyOf(suggestions);
    }

    private static List<String> teamIdSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (TeamDefinition team : TeamManager.INSTANCE.all()) {
            suggestions.add(team.token().toString());
        }
        return List.copyOf(suggestions);
    }

    private static List<String> gameTeamSuggestions(String instanceToken) {
        return List.of();
    }

    private static Identifier resolveGameTeamId(GameInstance<?, ?, ?> instance, Identifier id) {
        return null;
    }

    private static List<String> gameDefinitionSuggestions() {
        return GameManager.INSTANCE.getDefinitions().stream().map(d -> d.id().toString()).toList();
    }

    private static List<String> gameInstanceSuggestions() {
        return GameManager.INSTANCE.getInstances().stream().map(GameInstance::getInstanceId).toList();
    }

    private static String renderFlattenedPlayerPermissions(String playerName, java.util.UUID playerId, List<String> groups, Identifier fieldId, Identifier dimensionId) {
        StringBuilder builder = new StringBuilder();
        builder.append("permission=player\n")
                .append("player=").append(playerName)
                .append("\ngroups=").append(String.join("|", groups))
                .append("\nfield=").append(fieldId == null ? "-" : fieldId)
                .append("\ndimension=").append(dimensionId == null ? "-" : dimensionId)
                .append("\n");
        for (PermissionAction action : PermissionAction.values()) {
            PermissionDecision decision = PermissionManager.INSTANCE.evaluate(playerId, groups, action, fieldId, dimensionId);
            builder.append(action.name()).append("=").append(decision.name()).append("\n");
        }
        return builder.toString().trim();
    }

    private static UUID toScopedMyulibUUID(String route, String token) {
        if (token == null || token.isBlank()) {
            return UUID.nameUUIDFromBytes((route == null || route.isBlank() ? "everyone" : route + "/everyone").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        String value = token.trim();
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes((route + ":" + value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static UUID toMyulibUUID(String token) {
        if (token == null || token.isBlank()) {
            return UUID.nameUUIDFromBytes("everyone".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        String value = token.trim();
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static UUID resolveStableUuidToken(String token) {
        if (token == null || token.isBlank()) {
            return UUID.nameUUIDFromBytes("everyone".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        String value = token.trim();
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static Identifier toScopedMyulibIdentifier(String route, String token) {
        if (token == null || token.isBlank()) {
            return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, route == null || route.isBlank() ? "everyone" : route + "/everyone");
        }
        String value = token.trim();
        if (!value.contains(":")) {
            if (value.startsWith(route + "/")) {
                return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, value);
            }
            return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, route + "/" + value);
        }
        Identifier parsed = Identifier.parse(value);
        return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, parsed.getPath());
    }

    private static Identifier toMyulibIdentifier(String token) {
        if (token == null || token.isBlank()) {
            return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "everyone");
        }
        String value = token.trim();
        if (!value.contains(":")) {
            return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, value);
        }
        Identifier parsed = Identifier.parse(value);
        return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, parsed.getPath());
    }
}
