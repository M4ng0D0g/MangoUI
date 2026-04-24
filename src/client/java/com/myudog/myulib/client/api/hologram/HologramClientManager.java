package com.myudog.myulib.client.api.hologram;

import com.myudog.myulib.api.hologram.HologramDefinition;
import java.util.ArrayList;
import java.util.List;

public final class HologramClientManager {

    public static final HologramClientManager INSTANCE = new HologramClientManager();

    private HologramClientManager() {}
    private final List<HologramDefinition> ACTIVE_HOLOGRAMS = new ArrayList<>();

    public void update(List<HologramDefinition> newHolograms) {
        ACTIVE_HOLOGRAMS.clear();
        ACTIVE_HOLOGRAMS.addAll(newHolograms);
    }

    public List<HologramDefinition> getActiveHolograms() {
        return ACTIVE_HOLOGRAMS;
    }
}