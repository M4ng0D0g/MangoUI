package com.myudog.myulib.api;

import com.myudog.myulib.api.control.ControlManager;
import com.myudog.myulib.api.debug.DebugLogManager;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.field.FieldVisualizationManager;
import com.myudog.myulib.api.hologram.network.HologramNetworking;
import com.myudog.myulib.api.game.core.GameManager;
import com.myudog.myulib.api.game.examples.TicTacToeGameDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.camera.CameraApi;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.team.TeamManager;
import com.myudog.myulib.api.timer.TimerManager;
import com.myudog.myulib.api.ui.network.ConfigUiNetworking;

public final class MyulibApi {
	private MyulibApi() {
	}

	public static void init() {
		AccessSystems.init();
		DebugLogManager.INSTANCE.install();

		CameraApi.initServer();
		ControlManager.INSTANCE.install();
		HologramNetworking.registerPayloads();
		ConfigUiNetworking.registerPayloads();
		ConfigUiNetworking.registerServerReceivers();

		FieldManager.INSTANCE.install();
		FieldVisualizationManager.INSTANCE.install();
		GameManager.install();
		GameManager.register(new TicTacToeGameDefinition(TicTacToeGameDefinition.GAME_ID));
		PermissionManager.INSTANCE.install();
		RoleGroupManager.INSTANCE.install();
		TeamManager.INSTANCE.install();
		TimerManager.INSTANCE.install();
	}
}
