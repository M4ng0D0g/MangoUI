package com.myudog.myulib.api;

import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.game.GameManager;
import com.myudog.myulib.api.identity.IdentityManager;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.camera.CameraApi;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.team.TeamManager;
import com.myudog.myulib.api.timer.TimerManager;

public final class MyulibApi {
	private MyulibApi() {
	}

	public static void init() {
		GameManager.install();
		TimerManager.install();
		CameraApi.initServer();
		AccessSystems.init();
		FieldManager.install();
		TeamManager.install();
		IdentityManager.install();
		RoleGroupManager.install();
		PermissionManager.install();
	}
}
