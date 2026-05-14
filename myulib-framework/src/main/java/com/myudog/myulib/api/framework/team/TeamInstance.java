package com.myudog.myulib.api.framework.team;

import com.myudog.myulib.api.core.ecs.IComponent;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamInstance implements IComponent {
    public final TeamDefinition definition;
    public final Set<UUID> participants = ConcurrentHashMap.newKeySet();

    public TeamInstance(TeamDefinition definition) {
        this.definition = definition;
    }
}
