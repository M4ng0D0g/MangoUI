package com.myudog.myulib.client.api;

import com.myudog.myulib.client.api.camera.ClientCameraBridge;
import com.myudog.myulib.client.api.control.ClientControlManager;
import com.myudog.myulib.client.api.field.FieldVisualizationClientRenderer;
import com.myudog.myulib.client.api.ui.ConfigUiKeybinds;
import com.myudog.myulib.client.api.ui.network.ConfigUiClientNetworking;
import com.myudog.myulib.api.ui.network.ConfigUiNetworking;

public class MyulibApiClient {
	private MyulibApiClient() {
	}

	public static void init() {
		ClientCameraBridge.installBridge();
		ClientControlManager.INSTANCE.install();
		FieldVisualizationClientRenderer.install();
		ConfigUiNetworking.registerPayloads();
		ConfigUiClientNetworking.install();
		ConfigUiKeybinds.install();
	}
}
