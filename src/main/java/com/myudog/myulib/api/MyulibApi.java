package com.myudog.myulib.api;

import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.identity.IdentityManager;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.camera.CameraApi;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.team.TeamManager;
import com.myudog.myulib.api.game.Game;

public final class MyulibApi {
	private MyulibApi() {
	}

	public static void init() {
		Game.init();
		CameraApi.initServer();
		AccessSystems.init();
		FieldManager.install();
		TeamManager.install();
		IdentityManager.install();
		RoleGroupManager.install();
		PermissionManager.install();
	}
}
