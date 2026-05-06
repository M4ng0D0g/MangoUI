package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.game.features.*;

public abstract class GameData {

    public EcsFeature ECS_FEATURE;
    public FieldFeature FIELD_FEATURE;
    public ObjectFeature OBJECT_FEATURE;
    public TeamFeature TEAM_FEATURE;

    protected GameData() {}

    public GameData withEcs(EcsFeature ecsFeature) {
        this.ECS_FEATURE = ecsFeature;
        return this;
    }

    public GameData withField( FieldFeature fieldFeature) {
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
        ECS_FEATURE.clean(instance);
        FIELD_FEATURE.clean(instance);
        OBJECT_FEATURE.clean(instance);
        TEAM_FEATURE.clean(instance);
    }




}
