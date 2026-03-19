package com.myudog.mangoui

import com.myudog.mangoui.api.MangoAPI
import com.myudog.mangoui.internal.EcsWorld
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory


object MangoUI : ModInitializer {

	val LOGGER: Logger = LoggerFactory.getLogger("mango-ui")

	// 內部世界實例，不直接對外開放
	@PublishedApi
	internal lateinit var internalWorld: EcsWorld
		private set

	override fun onInitialize() {
		LOGGER.info("Mango UI is initializing...")

		// 1. 初始化內部 ECS 世界
		internalWorld = EcsWorld()

		// 2. 這裡可以預留註冊全域系統 (Systems) 的位置
		// 例如：LayoutSystem, AnimationSystem 等

		LOGGER.info("Mango UI (by MyuDog) has been initialized successfully.")
	}
}