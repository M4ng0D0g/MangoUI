package com.myudog.myulib.api.core;

/**
 * Unit
 *
 * 系統：核心基礎類型 (Core API - Constants)
 * 角色：提供時間單位的常數定義，主要以 Minecraft 刻 (Tick) 為基準。
 * 類型：Utility / Constants
 *
 * 預設 1 秒 = 20 Ticks。可用於計時器系統、冷卻時間計算等。
 */
public class Unit {

    /** 1 秒 (20 Ticks) */
    public static final long SECOND = 20;

    /** 1 分鐘 */
    public static final long MINUTE = SECOND * 60;

    /** 1 小時 */
    public static final long HOUR = MINUTE * 60;

    /** 1 天 */
    public static final long DAY = HOUR * 24;

    /** 1 週 */
    public static final long WEEK = DAY * 7;

    /** 1 個月 (以 30 天計) */
    public static final long MONTH = DAY * 30;

    /** 1 年 (以 365 天計) */
    public static final long YEAR = DAY * 365;
}
