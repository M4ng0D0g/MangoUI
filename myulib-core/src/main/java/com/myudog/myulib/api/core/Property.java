package com.myudog.myulib.api.core;

import java.util.function.Function;

/**
 * Property
 *
 * 系統：核心基礎類型 (Core API - Metadata)
 * 角色：代表一個通用的屬性定義，負責封裝屬性的詮釋資料 (Metadata) 與字串解析邏輯。
 * 類型：Data Structure / Record
 *
 * 此資料結構的核心職責為：
 * 1. 屬性識別：集中管理該屬性的名稱 (name) 與目標型別資訊 (type)。
 * 2. 轉型策略：提供將純文字字串安全轉換為具體 Java 型別 的標準介面。
 *
 * @param <T>    此屬性最終對應的 Java 泛型型別 (例如 Integer, Boolean 等)。
 * @param name   屬性的定義名稱或鍵值 (Key)。
 * @param type   屬性的目標類別，保留於執行期供反射或型別驗證使用。
 * @param parser 具體的解析策略函式，負責定義如何將傳入的 String 轉換為目標型別 T。
 */
public record Property<T>(
        String name,
        Class<T> type,
        Function<String, T> parser
) {
    /**
     * 將傳入的字串輸入，透過預先定義好的解析策略轉換為目標型別。
     *
     * @param input 來自外部的原始純文字字串
     * @return 轉換後的目標型別 T 的實例
     */
    public T parse(String input) {
        return parser.apply(input);
    }
}