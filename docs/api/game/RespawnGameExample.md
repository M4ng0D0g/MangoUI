# RespawnGameExample API 參考

## 類別
`class RespawnGameExample extends GameDefinition<RespawnGameExample.RespawnGameState>`

## 內嵌 enum
`RespawnGameState`
- `WAITING`
- `COUNTDOWN`
- `ACTIVE`
- `FINISHED`

## 主要行為
- `getInitialState()`：回傳 `WAITING`
- `getAllowedTransitions()`：定義 WAITING / COUNTDOWN / ACTIVE / FINISHED 的轉移表
- `createContext(...)`：建立這個遊戲需要的 runtime context

## 設計重點
- 這個範例只示範狀態與轉移表；feature / logic / component 的組裝不再放在 `GameDefinition` 內
- `myulib:respawn_anchor` 之類的 bootstrap 資料應由 context 或對應系統處理

## 用法
```java
GameManager.register(new RespawnGameExample());
```

