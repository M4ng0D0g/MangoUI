package com.myudog.myulib.api.framework.game.core;

import com.myudog.myulib.api.core.ecs.EcsContainer;
import com.myudog.myulib.api.framework.game.features.*;

public abstract class GameData {

    public EcsFeature ECS_FEATURE;
    public FieldFeature FIELD_FEATURE;
    public ObjectFeature OBJECT_FEATURE;
    public TeamFeature TEAM_FEATURE;

    protected GameData() {}

    public EcsContainer getEcsContainer() {
        return ECS_FEATURE.getContainer();
    }

    public GameData withEcs(EcsFeature ecsFeature) {
        this.ECS_FEATURE = ecsFeature;
        return this;
    }

    public GameData withField(FieldFeature fieldFeature) {
        this.FIELD_FEATURE = fieldFeature;
        return this;
    }

    public GameData withObject(ObjectFeature objectFeature) {
        this.OBJECT_FEATURE = objectFeature;
        return this;
    }

    public GameData withTeam(TeamFeature teamFeature) {
        this.TEAM_FEATURE = teamFeature;
        return this;
    }

    public void clean(GameInstance<?, ?, ?> instance) {
        if (ECS_FEATURE != null) ECS_FEATURE.clean(instance);
        if (FIELD_FEATURE != null) FIELD_FEATURE.clean(instance);
        if (OBJECT_FEATURE != null) OBJECT_FEATURE.clean(instance);
        if (TEAM_FEATURE != null) TEAM_FEATURE.clean(instance);
    }
}
