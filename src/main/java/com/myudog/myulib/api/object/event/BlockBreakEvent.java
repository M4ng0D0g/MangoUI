package com.myudog.myulib.api.object.event;

import com.myudog.myulib.api.event.IEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class BlockBreakEvent implements IEvent {
    private final ServerPlayer player;
    private final BlockPos pos;
    private final ServerLevel level;
    private boolean canceled;

    public BlockBreakEvent(ServerPlayer player, BlockPos pos, ServerLevel level) {
        this.player = player;
        this.pos = pos;
        this.level = level;
        this.canceled = false;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}

