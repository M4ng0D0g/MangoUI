package com.myudog.myulib.api.command;

import com.myudog.myulib.api.field.FieldAdminService;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.identity.IdentityAdminService;
import com.myudog.myulib.api.identity.IdentityGroupDefinition;
import com.myudog.myulib.api.permission.PermissionAdminService;
import com.myudog.myulib.api.permission.PermissionContext;
import com.myudog.myulib.api.permission.PermissionGrant;
import com.myudog.myulib.api.permission.PermissionLayer;
import com.myudog.myulib.api.permission.PermissionResolution;
import com.myudog.myulib.api.rolegroup.RoleGroupAdminService;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.team.TeamAdminService;
import com.myudog.myulib.api.team.TeamDefinition;
import com.myudog.myulib.api.ui.ConfigurationUiBridge;
import com.myudog.myulib.api.ui.ConfigurationUiRegistry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class AccessCommandService {
    private AccessCommandService() {
    }

    public static CommandResult createField(FieldDefinition field) {
        FieldAdminService.create(field);
        return CommandResult.success("Field created: " + field.id());
    }

    public static CommandResult updateField(String fieldId, UnaryOperator<FieldDefinition> updater) {
        return FieldAdminService.update(fieldId, updater) != null
            ? CommandResult.success("Field updated: " + fieldId)
            : CommandResult.failure("Missing field: " + fieldId);
    }

    public static CommandResult deleteField(String fieldId) {
        return FieldAdminService.delete(fieldId) != null
            ? CommandResult.success("Field deleted: " + fieldId)
            : CommandResult.failure("Missing field: " + fieldId);
    }

    public static CommandResult openFieldEditor(String fieldId, ConfigurationUiBridge ui) {
        FieldAdminService.openEditor(fieldId, ui);
        return CommandResult.success("Open field editor: " + fieldId);
    }

    public static CommandResult createIdentityGroup(IdentityGroupDefinition group) {
        IdentityAdminService.create(group);
        return CommandResult.success("Identity group created: " + group.id());
    }

    public static CommandResult updateIdentityGroup(String groupId, UnaryOperator<IdentityGroupDefinition> updater) {
        return IdentityAdminService.update(groupId, updater) != null
            ? CommandResult.success("Identity group updated: " + groupId)
            : CommandResult.failure("Missing identity group: " + groupId);
    }

    public static CommandResult deleteIdentityGroup(String groupId) {
        return IdentityAdminService.delete(groupId) != null
            ? CommandResult.success("Identity group deleted: " + groupId)
            : CommandResult.failure("Missing identity group: " + groupId);
    }

    public static CommandResult openIdentityGroupEditor(String groupId, ConfigurationUiBridge ui) {
        IdentityAdminService.openEditor(groupId, ui);
        return CommandResult.success("Open identity editor: " + groupId);
    }

    public static CommandResult createRoleGroup(String groupId, String displayName, int priority) {
        RoleGroupDefinition group = new RoleGroupDefinition(groupId, displayName, priority, List.of(), Map.of());
        RoleGroupAdminService.create(group);
        return CommandResult.success("Role group created: " + group.id());
    }

    public static CommandResult updateRoleGroup(String groupId, String displayName, Integer priority) {
        RoleGroupDefinition updated = RoleGroupAdminService.update(groupId, current -> new RoleGroupDefinition(
            current.id(),
            displayName == null || displayName.isBlank() ? current.displayName() : displayName,
            priority == null ? current.priority() : priority,
            current.grants(),
            current.metadata()
        ));
        return updated != null
            ? CommandResult.success("Role group updated: " + updated.id())
            : CommandResult.failure("Missing role group: " + groupId);
    }

    public static CommandResult deleteRoleGroup(String groupId) {
        return RoleGroupAdminService.delete(groupId) != null
            ? CommandResult.success("Role group deleted: " + groupId)
            : CommandResult.failure("Missing role group: " + groupId);
    }

    public static CommandResult getRoleGroup(String groupId) {
        RoleGroupDefinition group = RoleGroupManager.get(groupId);
        return group == null
            ? CommandResult.failure("Missing role group: " + groupId)
            : CommandResult.success(formatRoleGroup(group));
    }

    public static CommandResult listRoleGroups() {
        List<RoleGroupDefinition> groups = RoleGroupManager.list();
        if (groups.isEmpty()) {
            return CommandResult.success("Role groups: (none)");
        }
        String summary = groups.stream().map(group -> group.id() + "=" + group.displayName()).collect(Collectors.joining(", "));
        return CommandResult.success("Role groups: " + summary);
    }

    public static CommandResult openRoleGroupEditor(String groupId, ConfigurationUiBridge ui) {
        RoleGroupAdminService.openEditor(groupId, ui);
        return CommandResult.success("Open role group editor: " + groupId);
    }

    public static CommandResult createTeam(TeamDefinition team) {
        TeamAdminService.create(team);
        return CommandResult.success("Team created: " + team.id());
    }

    public static CommandResult updateTeam(String teamId, UnaryOperator<TeamDefinition> updater) {
        return TeamAdminService.update(teamId, updater) != null
            ? CommandResult.success("Team updated: " + teamId)
            : CommandResult.failure("Missing team: " + teamId);
    }

    public static CommandResult deleteTeam(String teamId) {
        return TeamAdminService.delete(teamId) != null
            ? CommandResult.success("Team deleted: " + teamId)
            : CommandResult.failure("Missing team: " + teamId);
    }

    public static CommandResult openTeamEditor(String teamId, ConfigurationUiBridge ui) {
        TeamAdminService.openEditor(teamId, ui);
        return CommandResult.success("Open team editor: " + teamId);
    }

    public static CommandResult grantPermission(PermissionLayer layer, String scopeId, PermissionGrant grant, UUID playerId) {
        switch (layer) {
            case GLOBAL -> PermissionAdminService.grantGlobal(grant);
            case DIMENSION -> PermissionAdminService.grantDimension(scopeId, grant);
            case FIELD -> PermissionAdminService.grantField(scopeId, grant);
            case USER -> PermissionAdminService.grantUser(playerId, grant);
        }
        return CommandResult.success("Permission granted: " + grant.id());
    }

    public static CommandResult evaluatePermission(PermissionContext context) {
        PermissionResolution resolution = PermissionAdminService.evaluate(context);
        return CommandResult.success(resolution.decision().name());
    }

    public static void registerDefaults() {
        CommandRegistry.register("field.open", ctx -> openFieldEditor(ctx.arguments().getOrDefault("id", ""), ConfigurationUiRegistry.bridge()));
        CommandRegistry.register("identity.open", ctx -> openIdentityGroupEditor(ctx.arguments().getOrDefault("id", ""), ConfigurationUiRegistry.bridge()));
        CommandRegistry.register("rolegroup.create", ctx -> createRoleGroup(
            ctx.arguments().getOrDefault("id", ""),
            ctx.arguments().getOrDefault("name", ctx.arguments().getOrDefault("displayName", ctx.arguments().getOrDefault("id", ""))),
            parseInt(ctx.arguments().getOrDefault("priority", "0"), 0)
        ));
        CommandRegistry.register("rolegroup.update", ctx -> updateRoleGroup(
            ctx.arguments().getOrDefault("id", ""),
            ctx.arguments().get("name"),
            ctx.arguments().containsKey("priority") ? parseInt(ctx.arguments().get("priority"), 0) : null
        ));
        CommandRegistry.register("rolegroup.delete", ctx -> deleteRoleGroup(ctx.arguments().getOrDefault("id", "")));
        CommandRegistry.register("rolegroup.get", ctx -> getRoleGroup(ctx.arguments().getOrDefault("id", "")));
        CommandRegistry.register("rolegroup.list", ctx -> listRoleGroups());
        CommandRegistry.register("rolegroup.open", ctx -> openRoleGroupEditor(ctx.arguments().getOrDefault("id", ""), ConfigurationUiRegistry.bridge()));
        CommandRegistry.register("team.open", ctx -> openTeamEditor(ctx.arguments().getOrDefault("id", ""), ConfigurationUiRegistry.bridge()));
    }

    private static String formatRoleGroup(RoleGroupDefinition group) {
        return "Role group{" +
            "id='" + group.id() + '\'' +
            ", name='" + group.displayName() + '\'' +
            ", priority=" + group.priority() +
            ", grants=" + group.grants().size() +
            ", metadata=" + group.metadata().size() +
            '}';
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}

