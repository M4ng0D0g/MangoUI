package com.myudog.myulib.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.control.ControlManager;
import com.myudog.myulib.api.control.ControlType;
import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import com.myudog.myulib.api.debug.DebugTraceManager;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldVisualizationManager;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.hologram.HologramDefinition;
import com.myudog.myulib.api.hologram.HologramManager;
import com.myudog.myulib.api.hologram.HologramFeature;
import com.myudog.myulib.api.hologram.network.HologramNetworking;
import com.myudog.myulib.api.game.core.GameInstance;
import com.myudog.myulib.api.game.core.GameConfig;
import com.myudog.myulib.api.game.core.GameManager;
import com.myudog.myulib.api.game.examples.TicTacToeGameDefinition;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.permission.ScopeLayer;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.team.TeamColor;
import com.myudog.myulib.api.team.TeamDefinition;
import com.myudog.myulib.api.team.TeamManager;
import com.myudog.myulib.api.timer.TimerDefinition;
import com.myudog.myulib.api.timer.TimerManager;
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

public class AccessCommandService {
    public static final String COMMAND_PREFIX = Myulib.MOD_ID + ":";
    private static final Set<Identifier> TRACKED_TIMERS = new LinkedHashSet<>();
    private static final Identifier TICTACTOE_BLUE_TEAM_ID = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "tictactoe_blue");

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
        CommandRegistry.register(COMMAND_PREFIX + "game:count", context -> CommandResult.success("game_instance=" + GameManager.getInstances().size()));
        CommandRegistry.register(COMMAND_PREFIX + "game:init", context -> {
            String token = context.arguments().getOrDefault("id", "");
            var resolved = GameManager.resolveInstanceId(token);
            if (resolved.isEmpty()) {
                return CommandResult.failure("game=not_found");
            }
            try {
                int instanceId = resolved.getAsInt();
                boolean initialized = GameManager.initInstance(instanceId);
                return initialized
                        ? CommandResult.success("game=initialized:" + instanceId)
                        : CommandResult.failure("game=already_initialized_or_not_found");
            } catch (IllegalArgumentException ex) {
                return CommandResult.failure("game=init_invalid_config:" + ex.getMessage());
            } catch (Exception ex) {
                return CommandResult.failure("game=init_failed:" + ex.getClass().getSimpleName());
            }
        });
        CommandRegistry.register(COMMAND_PREFIX + "game:start", context -> {
            String token = context.arguments().getOrDefault("id", "");
            var resolved = GameManager.resolveInstanceId(token);
            if (resolved.isEmpty()) {
                return CommandResult.failure("game=not_found");
            }
            try {
                int instanceId = resolved.getAsInt();
                boolean started = GameManager.startInstance(instanceId);
                return started
                        ? CommandResult.success("game=started:" + instanceId)
                        : CommandResult.failure("game=already_started_or_not_found");
            } catch (IllegalArgumentException ex) {
                return CommandResult.failure("game=init_invalid_config:" + ex.getMessage());
            } catch (Exception ex) {
                return CommandResult.failure("game=start_failed:" + ex.getClass().getSimpleName());
            }
        });
        CommandRegistry.register(COMMAND_PREFIX + "game:end", context -> {
            String token = context.arguments().getOrDefault("id", "");
            var resolved = GameManager.resolveInstanceId(token);
            if (resolved.isEmpty()) {
                return CommandResult.failure("game=not_found");
            }
            int instanceId = resolved.getAsInt();
            boolean ended = GameManager.shutdownInstance(instanceId);
            return ended
                    ? CommandResult.success("game=ended:" + instanceId)
                    : CommandResult.failure("game=end_failed_or_not_found");
        });
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
        CommandRegistry.register(COMMAND_PREFIX + "game:read", context -> {
            String token = context.arguments().getOrDefault("id", "");
            GameInstance<?, ?, ?> instance = GameManager.getInstance(token);
            if (instance == null) {
                return CommandResult.failure("game=not_found");
            }
            int instanceId = instance.getInstanceId();
            return CommandResult.success("game=instance:" + instanceId
                    + ",enabled:" + instance.isEnabled()
                    + ",initialized:" + instance.isInitialized()
                    + ",started:" + instance.isStarted());
        });
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
                                                                            Identifier fieldId = Identifier.parse(StringArgumentType.getString(context, "id"));
                                                                            fieldId = toMyulibIdentifier(fieldId.toString());
                                                                            Identifier dimId = context.getSource().getLevel().dimension().identifier();
                                                                            double x1 = DoubleArgumentType.getDouble(context, "x1");
                                                                            double y1 = DoubleArgumentType.getDouble(context, "y1");
                                                                            double z1 = DoubleArgumentType.getDouble(context, "z1");
                                                                            double x2 = DoubleArgumentType.getDouble(context, "x2");
                                                                            double y2 = DoubleArgumentType.getDouble(context, "y2");
                                                                            double z2 = DoubleArgumentType.getDouble(context, "z2");
                                                                            AABB bounds = cuboidFromCorners(x1, y1, z1, x2, y2, z2);
                                                                            createField(new FieldDefinition(fieldId, dimId, bounds, Map.of("source", "command")));
                                                                            return reply(context.getSource(), "field=create:" + fieldId);
                                                                        })))))))));

        root.then(Commands.literal("read")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(fieldIdSuggestions(), builder))
                        .executes(context -> {
                            Identifier fieldId = resolveFieldIdToken(StringArgumentType.getString(context, "id"));
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
                            Identifier fieldId = resolveFieldIdToken(StringArgumentType.getString(context, "id"));
                            if (fieldId == null) {
                                return reply(context.getSource(), "field=not_found");
                            }
                            FieldDefinition existing = FieldManager.INSTANCE.get(fieldId);
                            if (existing == null) {
                                return reply(context.getSource(), "field=not_found");
                            }
                            FieldManager.INSTANCE.unregister(fieldId);
                            createField(new FieldDefinition(existing.id(), existing.dimensionId(), existing.bounds(), Map.of("updated", "true")));
                            return reply(context.getSource(), "field=updated:" + fieldId);
                        })));

        root.then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(fieldIdSuggestions(), builder))
                        .executes(context -> {
                            Identifier fieldId = resolveFieldIdToken(StringArgumentType.getString(context, "id"));
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
                                                                            Identifier id = Identifier.parse(StringArgumentType.getString(context, "id"));
                                                                            id = toMyulibIdentifier(id.toString());
                                                                            Identifier dimId = context.getSource().getLevel().dimension().identifier();
                                                                            double x1 = DoubleArgumentType.getDouble(context, "x1");
                                                                            double y1 = DoubleArgumentType.getDouble(context, "y1");
                                                                            double z1 = DoubleArgumentType.getDouble(context, "z1");
                                                                            double x2 = DoubleArgumentType.getDouble(context, "x2");
                                                                            double y2 = DoubleArgumentType.getDouble(context, "y2");
                                                                            double z2 = DoubleArgumentType.getDouble(context, "z2");
                                                                            AABB bounds = HologramManager.INSTANCE.cuboidFromCorners(x1, y1, z1, x2, y2, z2);
                                                                            HologramManager.INSTANCE.register(new HologramDefinition(id, dimId, bounds, id.toString()));
                                                                            return reply(context.getSource(), "hologram=create:" + id);
                                                                        })))))))));

        root.then(Commands.literal("read")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(projectionIdSuggestions(), builder))
                        .executes(context -> {
                            Identifier id = resolveProjectionIdToken(StringArgumentType.getString(context, "id"));
                            HologramDefinition projection = id == null ? null : HologramManager.INSTANCE.get(id);
                            if (projection == null) {
                                return reply(context.getSource(), "hologram=not_found");
                            }
                            return reply(context.getSource(), "hologram=id:" + projection.id() + ",dim:" + projection.dimensionId());
                        })));

        root.then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(projectionIdSuggestions(), builder))
                        .executes(context -> {
                            Identifier id = resolveProjectionIdToken(StringArgumentType.getString(context, "id"));
                            if (id == null || HologramManager.INSTANCE.get(id) == null) {
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
                            Identifier fieldId = FieldManager.INSTANCE.findAt(dimId, player.position()).map(FieldDefinition::id).orElse(null);
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
                                            Identifier id = toMyulibIdentifier(StringArgumentType.getString(context, "id"));
                                            int priority = IntegerArgumentType.getInteger(context, "priority");
                                            createRoleGroup(id, Component.literal(id.toString()), priority);
                                            return reply(context.getSource(), "rolegroup=create:" + id);
                                        }))))
                .then(Commands.literal("read")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(rolegroupIdSuggestions(), builder))
                                .executes(context -> {
                                    Identifier id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
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
                                            Identifier id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
                                            if (RoleGroupManager.INSTANCE.get(id) == null) {
                                                return reply(context.getSource(), "rolegroup=not_found");
                                            }
                                            int priority = IntegerArgumentType.getInteger(context, "priority");
                                            RoleGroupManager.INSTANCE.update(id, old -> new RoleGroupDefinition(old.id(), old.translationKey(), priority, old.metadata(), old.members()));
                                            return reply(context.getSource(), "rolegroup=updated:" + id);
                                        }))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(rolegroupIdSuggestions(), builder))
                                .executes(context -> {
                                    Identifier id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
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
                                            Identifier id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
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
                                            Identifier id = resolveRoleGroupIdToken(StringArgumentType.getString(context, "id"));
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
                                            Identifier id = Identifier.parse(StringArgumentType.getString(context, "id"));
                                            id = toMyulibIdentifier(id.toString());
                                            TeamColor color = parseTeamColor(StringArgumentType.getString(context, "color"));
                                            TeamManager.INSTANCE.register(new TeamDefinition(id, Component.literal(id.toString()), color, Map.of()));
                                            return reply(context.getSource(), "team=create:" + id);
                                        }))))
                .then(Commands.literal("read")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(teamIdSuggestions(), builder))
                                .executes(context -> {
                                    Identifier id = resolveTeamIdToken(StringArgumentType.getString(context, "id"));
                                    TeamDefinition team = id == null ? null : TeamManager.INSTANCE.get(id);
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
                                            Identifier id = resolveTeamIdToken(StringArgumentType.getString(context, "id"));
                                            if (id == null || TeamManager.INSTANCE.get(id) == null) {
                                                return reply(context.getSource(), "team=not_found");
                                            }
                                            TeamColor color = parseTeamColor(StringArgumentType.getString(context, "color"));
                                            TeamManager.INSTANCE.update(id, old -> new TeamDefinition(old.id(), old.translationKey(), color, old.flags()));
                                            return reply(context.getSource(), "team=updated:" + id);
                                        }))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(teamIdSuggestions(), builder))
                                .executes(context -> {
                                    Identifier id = resolveTeamIdToken(StringArgumentType.getString(context, "id"));
                                    if (id == null || TeamManager.INSTANCE.get(id) == null) {
                                        return reply(context.getSource(), "team=not_found");
                                    }
                                    TeamManager.INSTANCE.unregister(id);
                                    return reply(context.getSource(), "team=deleted:" + id);
                                })))
                .then(Commands.literal("list")
                        .executes(context -> reply(context.getSource(), "team=count:" + TeamManager.INSTANCE.all().size()))));
    }

    private static void registerGameCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND_PREFIX + "game")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("create")
                        .then(Commands.literal("tictactoe")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) {
                                                return reply(context.getSource(), "game=tictactoe_player_only");
                                            }

                                            String requestedName = normalizeGameInstanceName(StringArgumentType.getString(context, "id"));
                                            if (GameManager.resolveInstanceId(requestedName).isPresent()) {
                                                return reply(context.getSource(), "game=instance_name_exists:" + requestedName);
                                            }

                                            ensureTicTacToeBlueTeamExists();
                                            Set<UUID> members = TeamManager.INSTANCE.members(TICTACTOE_BLUE_TEAM_ID);
                                            UUID bluePlayerId;
                                            if (members.isEmpty()) {
                                                bluePlayerId = player.getUUID();
                                                TeamManager.INSTANCE.addPlayer(TICTACTOE_BLUE_TEAM_ID, bluePlayerId);
                                            } else if (members.size() == 1) {
                                                bluePlayerId = members.iterator().next();
                                            } else {
                                                return reply(context.getSource(), "game=tictactoe_blue_must_have_exactly_one_player");
                                            }

                                            try {
                                                TicTacToeGameDefinition.TicTacToeConfig config = TicTacToeGameDefinition.TicTacToeConfig.fromStart(player.position(), bluePlayerId);
                                                GameInstance<?, ?, ?> instance = GameManager.createInstance(TicTacToeGameDefinition.GAME_ID, requestedName, context.getSource().getLevel());
                                                GameManager.setInstanceConfig(requestedName, config);
                                                return reply(context.getSource(), "game=tictactoe_created_waiting:" + requestedName + "->" + instance.getInstanceId());
                                            } catch (Exception ex) {
                                                return reply(context.getSource(), "game=tictactoe_create_failed:" + ex.getClass().getSimpleName());
                                            }
                                        }))))
                .then(Commands.literal("join")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("gameInstanceId", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            String token = StringArgumentType.getString(context, "gameInstanceId");
                                            GameInstance<?, ?, ?> instance = GameManager.getInstance(token);
                                            if (instance == null) {
                                                return reply(context.getSource(), "game=not_found");
                                            }

                                            if (!GameManager.joinPlayer(instance.getInstanceId(), target.getUUID(), null)) {
                                                return reply(context.getSource(), "game=join_failed");
                                            }
                                            Identifier assigned = instance.getPlayerTeam(target.getUUID()).orElse(GameConfig.SPECTATOR_TEAM_ID);
                                            return reply(context.getSource(),
                                                    "game=joined:" + target.getName().getString() + "->" + assigned.getPath() + "@" + instance.getInstanceId());
                                        })
                                        .then(Commands.argument("teamId", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    String token = StringArgumentType.getString(context, "gameInstanceId");
                                                    return SharedSuggestionProvider.suggest(gameTeamSuggestions(token), builder);
                                                })
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    String token = StringArgumentType.getString(context, "gameInstanceId");
                                                    GameInstance<?, ?, ?> instance = GameManager.getInstance(token);
                                                    if (instance == null) {
                                                        return reply(context.getSource(), "game=not_found");
                                                    }

                                                    Identifier requestedTeam = resolveGameTeamId(instance, StringArgumentType.getString(context, "teamId"));
                                                    if (requestedTeam == null) {
                                                        return reply(context.getSource(), "game=team_not_found");
                                                    }

                                                    if (!GameManager.joinPlayer(instance.getInstanceId(), target.getUUID(), requestedTeam)) {
                                                        return reply(context.getSource(), "game=join_failed");
                                                    }
                                                    Identifier assigned = instance.getPlayerTeam(target.getUUID()).orElse(GameConfig.SPECTATOR_TEAM_ID);
                                                    return reply(context.getSource(),
                                                            "game=joined:" + target.getName().getString() + "->" + assigned.getPath() + "@" + instance.getInstanceId());
                                                })))))
                .then(Commands.literal("host")
                        .then(Commands.literal("get")
                                .then(Commands.argument("gameInstanceId", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                        .executes(context -> {
                                            String token = StringArgumentType.getString(context, "gameInstanceId");
                                            GameInstance<?, ?, ?> instance = GameManager.getInstance(token);
                                            if (instance == null) {
                                                return reply(context.getSource(), "game=not_found");
                                            }
                                            String host = instance.getHostUuid().map(UUID::toString).orElse("none");
                                            return reply(context.getSource(), "game=host:" + instance.getInstanceId() + "=" + host);
                                        })))
                        .then(Commands.literal("set")
                                .then(Commands.argument("gameInstanceId", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                        .then(Commands.argument("playerId", StringArgumentType.word())
                                                .executes(context -> {
                                                    String token = StringArgumentType.getString(context, "gameInstanceId");
                                                    GameInstance<?, ?, ?> instance = GameManager.getInstance(token);
                                                    if (instance == null) {
                                                        return reply(context.getSource(), "game=not_found");
                                                    }

                                                    UUID playerId;
                                                    try {
                                                        playerId = UUID.fromString(StringArgumentType.getString(context, "playerId"));
                                                    } catch (IllegalArgumentException ex) {
                                                        return reply(context.getSource(), "game=invalid_player_id");
                                                    }

                                                    instance.setHostUuid(playerId);
                                                    return reply(context.getSource(), "game=host_set:" + instance.getInstanceId() + "=" + playerId);
                                                })))))
                .then(Commands.literal("init")
                        .then(Commands.argument("gameInstanceId", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                .executes(context -> {
                                    String token = StringArgumentType.getString(context, "gameInstanceId");
                                    GameInstance<?, ?, ?> instance = GameManager.getInstance(token);
                                    if (instance == null) {
                                        return reply(context.getSource(), "game=not_found");
                                    }
                                    int instanceId = instance.getInstanceId();

                                    try {
                                        boolean initialized = GameManager.initInstance(instanceId);
                                        return reply(context.getSource(), initialized
                                                ? "game=initialized:" + instanceId
                                                : "game=already_initialized_or_not_found");
                                    } catch (IllegalArgumentException ex) {
                                        return reply(context.getSource(), "game=init_invalid_config:" + ex.getMessage());
                                    } catch (Exception ex) {
                                        return reply(context.getSource(), "game=init_failed:" + ex.getClass().getSimpleName());
                                    }
                                })))
                .then(Commands.literal("start")
                        .then(Commands.argument("gameInstanceId", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                .executes(context -> {
                                    String token = StringArgumentType.getString(context, "gameInstanceId");
                                    GameInstance<?, ?, ?> instance = GameManager.getInstance(token);
                                    if (instance == null) {
                                        return reply(context.getSource(), "game=not_found");
                                    }
                                    int instanceId = instance.getInstanceId();

                                    try {
                                        boolean started = GameManager.startInstance(instanceId);
                                        return reply(context.getSource(), started
                                                ? "game=started:" + instanceId
                                                : "game=already_started_or_not_found");
                                    } catch (IllegalArgumentException ex) {
                                        return reply(context.getSource(), "game=start_invalid_config:" + ex.getMessage());
                                    } catch (Exception ex) {
                                        return reply(context.getSource(), "game=start_failed:" + ex.getClass().getSimpleName());
                                    }
                                })))
                .then(Commands.literal("end")
                        .then(Commands.argument("gameInstanceId", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                .executes(context -> {
                                    String token = StringArgumentType.getString(context, "gameInstanceId");
                                    GameInstance<?, ?, ?> instance = GameManager.getInstance(token);
                                    if (instance == null) {
                                        return reply(context.getSource(), "game=not_found");
                                    }
                                    int instanceId = instance.getInstanceId();

                                    try {
                                        boolean ended = GameManager.shutdownInstance(instanceId);
                                        return reply(context.getSource(), ended
                                                ? "game=ended:" + instanceId
                                                : "game=end_failed_or_not_found");
                                    } catch (Exception ex) {
                                        return reply(context.getSource(), "game=end_failed:" + ex.getClass().getSimpleName());
                                    }
                                })))
                .then(Commands.literal("read")
                        .then(Commands.argument("gameInstanceId", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                .executes(context -> {
                                    String token = StringArgumentType.getString(context, "gameInstanceId");
                                    GameInstance<?, ?, ?> instance = GameManager.getInstance(token);
                                    if (instance == null) {
                                        return reply(context.getSource(), "game=not_found");
                                    }
                                    int instanceId = instance.getInstanceId();
                                    return reply(context.getSource(), "game=instance:" + instanceId + ",enabled:" + instance.isEnabled() + ",initialized:" + instance.isInitialized() + ",started:" + instance.isStarted());
                                })))
                .then(Commands.literal("update")
                        .then(Commands.argument("gameInstanceId", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                .executes(context -> {
                                    String token = StringArgumentType.getString(context, "gameInstanceId");
                                    if (!GameManager.resetInstance(token)) {
                                        return reply(context.getSource(), "game=not_found");
                                    }
                                    int instanceId = GameManager.resolveInstanceId(token).orElse(-1);
                                    return reply(context.getSource(), "game=updated:" + instanceId);
                                })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("gameInstanceId", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(gameInstanceSuggestions(), builder))
                                .executes(context -> {
                                    String token = StringArgumentType.getString(context, "gameInstanceId");
                                    int instanceId = GameManager.resolveInstanceId(token).orElse(-1);
                                    boolean removed = GameManager.destroyInstance(token);
                                    return reply(context.getSource(), removed ? "game=deleted:" + instanceId : "game=not_found");
                                })))
                .then(Commands.literal("list")
                        .executes(context -> {
                            long started = GameManager.getInstances().stream().filter(GameInstance::isStarted).count();
                            return reply(context.getSource(), "game=instance_count:" + GameManager.getInstances().size() + ",started=" + started + ",named=" + GameManager.instanceTokens().size());
                        })));
    }

    private static void registerTimerCrud(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND_PREFIX + "timer")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("ticks", LongArgumentType.longArg(1L))
                                        .executes(context -> {
                                            Identifier id = Identifier.parse(StringArgumentType.getString(context, "id"));
                                            id = toMyulibIdentifier(id.toString());
                                            long ticks = LongArgumentType.getLong(context, "ticks");
                                            TimerManager.INSTANCE.register(new TimerDefinition(id, ticks));
                                            TRACKED_TIMERS.add(id);
                                            return reply(context.getSource(), "timer=create:" + id);
                                        }))))
                .then(Commands.literal("read")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(timerIdSuggestions(), builder))
                                .executes(context -> {
                                    Identifier id = resolveTimerIdToken(StringArgumentType.getString(context, "id"));
                                    return reply(context.getSource(), "timer=exists:" + TimerManager.INSTANCE.has(id));
                                })))
                .then(Commands.literal("update")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(timerIdSuggestions(), builder))
                                .then(Commands.argument("ticks", LongArgumentType.longArg(1L))
                                        .executes(context -> {
                                            Identifier id = resolveTimerIdToken(StringArgumentType.getString(context, "id"));
                                            long ticks = LongArgumentType.getLong(context, "ticks");
                                            if (!TimerManager.INSTANCE.has(id)) {
                                                return reply(context.getSource(), "timer=not_found");
                                            }
                                            TimerManager.INSTANCE.unregister(id);
                                            TimerManager.INSTANCE.register(new TimerDefinition(id, ticks));
                                            TRACKED_TIMERS.add(id);
                                            return reply(context.getSource(), "timer=updated:" + id);
                                        }))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(timerIdSuggestions(), builder))
                                .executes(context -> {
                                    Identifier id = resolveTimerIdToken(StringArgumentType.getString(context, "id"));
                                    boolean removed = TimerManager.INSTANCE.unregister(id) != null;
                                    TRACKED_TIMERS.remove(id);
                                    return reply(context.getSource(), removed ? "timer=deleted:" + id : "timer=not_found");
                                })))
                .then(Commands.literal("list")
                        .executes(context -> reply(context.getSource(), "timer=definition_count:" + TimerManager.INSTANCE.timerDefinitionCount() + ",tracked:" + TRACKED_TIMERS.size()))));
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

    private static Identifier resolveFieldIdToken(String token) {
        Identifier fromShort = FieldManager.INSTANCE.resolveShortId(token);
        return fromShort != null ? fromShort : toMyulibIdentifier(token);
    }

    private static void ensureTicTacToeBlueTeamExists() {
        if (TeamManager.INSTANCE.get(TICTACTOE_BLUE_TEAM_ID) == null) {
            TeamManager.INSTANCE.register(new TeamDefinition(
                    TICTACTOE_BLUE_TEAM_ID,
                    Component.literal("TicTacToe Blue"),
                    TeamColor.BLUE,
                    Map.of()
            ));
        }
    }

    private static Identifier resolveRoleGroupIdToken(String token) {
        Identifier fromShort = RoleGroupManager.INSTANCE.resolveShortId(token);
        if (fromShort != null) {
            return fromShort;
        }
        return toMyulibIdentifier(token);
    }

    private static Identifier resolveTeamIdToken(String token) {
        Identifier fromShort = TeamManager.INSTANCE.resolveShortId(token);
        return fromShort != null ? fromShort : toMyulibIdentifier(token);
    }

    private static Identifier resolveTimerIdToken(String token) {
        Identifier fromPath = TRACKED_TIMERS.stream()
                .filter(id -> id.getPath().equals(token))
                .findFirst()
                .orElse(null);
        return fromPath != null ? fromPath : toMyulibIdentifier(token);
    }

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
                + ",game_instance=" + GameManager.getInstances().size()
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
    public static void createRoleGroup(Identifier groupId, MutableComponent displayName, int priority) {
        Identifier normalized = toMyulibIdentifier(groupId == null ? "" : groupId.toString());
        RoleGroupDefinition group = new RoleGroupDefinition(normalized, displayName, priority, Map.of(), Set.of());
        RoleGroupManager.INSTANCE.register(group);
        RoleGroupManager.INSTANCE.save();
    }

    public static void deleteRoleGroup(Identifier groupId) {
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

    public static void deleteField(Identifier fieldId) {
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
                yield fieldId != null ? fieldId : toMyulibIdentifier(token);
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
        var field = fieldId == null ? null : FieldManager.INSTANCE.get(fieldId);
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
        for (Identifier id : HologramManager.INSTANCE.all().keySet()) {
            suggestions.add(id.getPath());
        }
        return List.copyOf(suggestions);
    }

    private static Identifier resolveProjectionIdToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        if (!token.contains(":")) {
            Identifier candidate = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, token);
            if (HologramManager.INSTANCE.get(candidate) != null) {
                return candidate;
            }
        }
        return toMyulibIdentifier(token);
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
            suggestions.add(group.id().getPath());
            String shortId = RoleGroupManager.INSTANCE.getShortIdOf(group.id());
            if (shortId != null && !shortId.isBlank()) {
                suggestions.add(shortId);
            }
        }
        return List.copyOf(suggestions);
    }

    private static List<String> fieldIdSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (Identifier id : FieldManager.INSTANCE.all().keySet()) {
            suggestions.add(id.getPath());
            String shortId = FieldManager.INSTANCE.getShortIdOf(id);
            if (shortId != null && !shortId.isBlank()) {
                suggestions.add(shortId);
            }
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
            suggestions.add(group.id().getPath());
            suggestions.add(PermissionManager.INSTANCE.normalizeGroupName(group.id().getPath()));
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
                String shortId = PermissionManager.INSTANCE.getDimensionShortIdOf(id);
                if (shortId != null && !shortId.isBlank()) {
                    suggestions.add(shortId);
                }
            }
        } else if (scope == ScopeLayer.FIELD) {
            for (Identifier id : FieldManager.INSTANCE.all().keySet()) {
                suggestions.add(id.getPath());
                String fieldShort = FieldManager.INSTANCE.getShortIdOf(id);
                if (fieldShort != null && !fieldShort.isBlank()) {
                    suggestions.add(fieldShort);
                }
            }
            for (Identifier id : PermissionManager.INSTANCE.fieldScopeIds()) {
                suggestions.add(id.getPath());
                String shortId = PermissionManager.INSTANCE.getFieldShortIdOf(id);
                if (shortId != null && !shortId.isBlank()) {
                    suggestions.add(shortId);
                }
            }
        } else if (scope == ScopeLayer.GLOBAL) {
            suggestions.add("global");
        }

        return List.copyOf(suggestions);
    }

    private static List<String> timerIdSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (Identifier id : TRACKED_TIMERS) {
            suggestions.add(id.getPath());
        }
        return List.copyOf(suggestions);
    }

    private static List<String> teamIdSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (TeamDefinition team : TeamManager.INSTANCE.all()) {
            suggestions.add(team.id().getPath());
            String shortId = TeamManager.INSTANCE.getShortIdOf(team.id());
            if (shortId != null && !shortId.isBlank()) {
                suggestions.add(shortId);
            }
        }
        return List.copyOf(suggestions);
    }

    private static List<String> gameTeamSuggestions(String instanceToken) {
        GameInstance<?, ?, ?> instance = GameManager.getInstance(instanceToken);
        if (instance == null) {
            return List.of();
        }

        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, Identifier> entry : instance.getConfig().teams().entrySet()) {
            suggestions.add(entry.getKey());
            suggestions.add(entry.getValue().getPath());
            suggestions.add(entry.getValue().toString());
        }
        return List.copyOf(suggestions);
    }

    private static Identifier resolveGameTeamId(GameInstance<?, ?, ?> instance, String token) {
        if (instance == null || token == null || token.isBlank()) {
            return null;
        }

        String normalized = token.trim();
        for (Map.Entry<String, Identifier> entry : instance.getConfig().teams().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(normalized)) {
                return entry.getValue();
            }
        }

        for (Identifier teamId : instance.getConfig().teams().values()) {
            if (teamId.getPath().equalsIgnoreCase(normalized) || teamId.toString().equalsIgnoreCase(normalized)) {
                return teamId;
            }
        }
        return null;
    }

    private static List<String> gameInstanceSuggestions() {
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();
        suggestions.addAll(GameManager.instanceTokens());
        for (GameInstance<?, ?, ?> instance : GameManager.getInstances()) {
            suggestions.add(String.valueOf(instance.getInstanceId()));
        }
        return List.copyOf(suggestions);
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
