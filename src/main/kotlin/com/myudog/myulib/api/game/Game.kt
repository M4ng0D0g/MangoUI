package com.myudog.myulib.api.game

import com.myudog.myulib.api.game.timer.TimerManager

/**
 * 遊戲系統總入口。
 *
 * 目前先初始化 timer 系統，後續其他 game feature 也可以從這裡掛載。
 */
object Game {
    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        TimerManager.install()
    }
}

