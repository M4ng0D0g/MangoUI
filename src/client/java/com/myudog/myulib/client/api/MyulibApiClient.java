package com.myudog.myulib.client.api;

import com.myudog.myulib.client.api.camera.ClientCameraBridge;

public class MyulibApiClient {
	private MyulibApiClient() {
	}

	public static void init() {
		ClientCameraBridge.installBridge();
	}
}
