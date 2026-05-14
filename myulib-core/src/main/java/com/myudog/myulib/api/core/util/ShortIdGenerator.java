package com.myudog.myulib.api.core.util;

import java.security.SecureRandom;

/**
 * ShortIdGenerator
 *
 * 系統：通用工具系統 (Utility System)
 * 角色：隨機短 ID 生成器。
 * 類型：Utility / Factory
 *
 * 此類別用於生成隨機且簡短的識別碼，特別優化以符合 Minecraft Identifier 的命名規範：
 * 1. 僅包含小寫字母 (a-z) 與數字 (0-9)。
 * 2. 使用 {@code SecureRandom} 確保隨機性的品質。
 * 適合用於生成動態實體 ID、會話 ID 或臨時檔案名。
 */
public final class ShortIdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";

    private static final int DEFAULT_LENGTH = 10;

    private ShortIdGenerator() {}

    /**
     * 生成預設長度 (10碼) 的隨機短 ID
     */
    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    /**
     * 自訂長度的隨機短 ID
     */
    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}