# GameBootstrapConfig
## Role
This page is the canonical reference for `GameBootstrapConfig` in the `game` docs area.
## Unique responsibility
It documents the public API, intent, and practical usage of this class without mixing in unrelated systems.
## Practical writing
Use this page when you need the class-level contract, then follow the field and method sections below.
## Fields
- Fields are listed in the existing API content below.
## Methods
- Methods are listed in the existing API content below.
GameBootstrapConfig API 參考

## 類別
`record GameBootstrapConfig`

## 欄位
- `specialObjects: Map<Identifier, GameObjectConfig>`：特殊物件的不可變快照
- `metadata: Map<String, String>`：建立遊戲時的額外 metadata，不可為 `null`

## 建構
```java
new GameBootstrapConfig();
new GameBootstrapConfig(Map.of(), Map.of());
new GameBootstrapConfig(specialObjects, metadata);
```

## 行為
- `null` 會被轉成空 map
- 內部會使用 `Map.copyOf(...)` 做不可變快照
- `GameManager.createInstance(...)` 會把這份資料交給對應的 `GameDefinition.createContext(...)`，由 context 與 runtime 系統完成組裝

## 用法
```java
GameBootstrapConfig config = new GameBootstrapConfig(
    Map.of(
        Identifier.of("myulib", "respawn_anchor"),
        new GameObjectConfig(
            Identifier.of("myulib", "respawn_anchor"),
            null,
            "Respawn Anchor",
            true
        )
    ),
    Map.of("mode", "arena")
);
```

