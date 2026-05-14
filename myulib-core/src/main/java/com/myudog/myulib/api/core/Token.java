package com.myudog.myulib.api.core;

import org.jetbrains.annotations.NotNull;

/**
 * Token
 *
 * 系統：核心基礎類型 (Core API - Messaging/ID)
 * 角色：具備路徑嵌套能力的識別令牌，用於標識分層級的資源或狀態。
 * 類型：Data Structure / Record
 *
 * Token 由「路徑 (path)」與「值 (value)」組成。支援透過父子關係自動合併路徑，
 * 適合用於動態生成標籤、分群組的事件 ID 等場景。
 */
public record Token(@NotNull String path, @NotNull String value) implements Tokenable {

    /**
     * 核心構造函數：確保路徑格式統一，去除首尾空白與多餘斜線。
     */
    public Token {
        path = path.replaceAll("^/|/$", "");
        value = value.trim();
    }

    /**
     * 變長參數構造函數。
     * 將最後一個參數視為 value，其餘部分合併為路徑。
     * 範例：new Token("game", "room1", "team1") -> path: "game/room1", value: "team1"
     *
     * @param args 路徑片段與值的陣列
     */
    public Token(@NotNull String... args) {
        this(
                args.length > 1 ? String.join("/", java.util.Arrays.copyOf(args, args.length - 1)) : "",
                args.length > 0 ? args[args.length - 1] : ""
        );
    }

    /**
     * Token 嵌套構造函數，實現路徑繼承。
     * 將父 Token 的完整標識作為子 Token 的路徑前綴。
     *
     * @param parent 父級 Token
     * @param value  此層級的值
     */
    public Token(@NotNull Tokenable parent, @NotNull String value) {
        this(
                parent.getPath().isEmpty() ? parent.getToken() : parent.getPath() + "/" + parent.getToken(),
                value
        );
    }

    @Override
    public @NotNull String getPath() {
        return path;
    }

    @Override
    public @NotNull String getToken() {
        return value;
    }
}