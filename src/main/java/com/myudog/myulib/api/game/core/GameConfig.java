package com.myudog.myulib.api.game.core;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.object.IObjectRt;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.team.TeamDefinition;
import com.myudog.myulib.api.team.TeamColor;
import com.myudog.myulib.api.team.TeamManager;
import com.myudog.myulib.api.timer.TimerDefinition;
import com.myudog.myulib.api.timer.TimerManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * [C] 遊戲設定藍圖 (Game Config)
 * 代表一局遊戲初始化所需的絕對靜態參數。
 * ⚠️ 必須保證為不可變 (Immutable)，且在遊戲實例建立後不得修改。
 */
public abstract class GameConfig {

    public final UUID SPECTATOR_TEAM = UUID.randomUUID();
    public final Identifier GAME_DEF_ID;

    public GameConfig(Identifier gameDefId) {
        this.GAME_DEF_ID = gameDefId;
    }

    public abstract boolean validate() throws Exception;

    public boolean allowSpectator() {
        return true;
    }

    public Set<UUID> teamsOf() {
        return Set.of(SPECTATOR_TEAM);
    }
}

