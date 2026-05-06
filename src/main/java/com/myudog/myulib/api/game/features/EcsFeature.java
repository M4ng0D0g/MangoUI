package com.myudog.myulib.api.game.features;

import com.myudog.myulib.api.ecs.EcsContainer;
import com.myudog.myulib.api.game.core.GameInstance;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface EcsFeature {
    EcsContainer getContainer();

    Optional<Integer> getEntity(@NotNull UUID uuid);

    int getOrCreateParticipant(@NotNull UUID uuid);

    int removeParticipant(@NotNull UUID uuid);

    void clean(GameInstance<?, ?, ?> instance);
}
