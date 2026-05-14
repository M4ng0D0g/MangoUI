package com.myudog.myulib.api.core;

/**
 * Color
 *
 * 系統：核心基礎類型 (Core API - Data Types)
 * 角色：定義顏色資訊的統一合約，支援 RGB 數值與分量提取。
 * 類型：Interface / Data Contract
 *
 * 用於 UI、粒子特效以及任何需要顏色標籤的系統。
 */
public interface Color {
    /**
     * 獲取打包後的 RGB 整數值 (0xRRGGBB)。
     *
     * @return RGB 數值
     */
    int rgb();

    default float red() {
        return ((rgb() >> 16) & 0xFF) / 255.0f;
    }

    default float green() {
        return ((rgb() >> 8) & 0xFF) / 255.0f;
    }

    default float blue() {
        return (rgb() & 0xFF) / 255.0f;
    }
}

