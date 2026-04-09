# Game System

The Game system is the high-level flow entry point. It manages game definitions, instances, and state transitions; `team` / `timer` / `object` are documented as standalone systems and only bridged here where necessary.

## 類別架構關係
- `Game` 是啟動入口，負責安裝 `GameManager`、`ComponentManager`、`TimerManager`；`Field` / `Identity` / `Permission` / `Team` 走獨立 access 架構。
- `GameManager` 是定義與 instance 的中央註冊表，也是 `tickAll()` 的全域驅動者。
- `GameDefinition` 是所有遊戲的最小契約基底，只定義 id、initial state、allowed transitions 與 `createContext(...)`；runtime 組裝交給 context 與對應系統。
- `GameInstance` 是實際執行中的遊戲容器，內含 feature store、special objects、components、logic；`team` / `timer` / `object` 走獨立系統文件。
- `GameObjectBindingFeature` / `GameObjectHooks` 負責遊戲物件 runtime 與 mixin/hook 分發。
- `GameTeamFeature` / `GameTeamDefinition` / `GameTeamColor` 負責程式碼建立的隊伍與成員綁定；access 類系統請看 `docs/access/`，team 主線請看 `docs/team/`。
- `LogicFactsResolver` 會從 `GameInstance` 與外部系統讀取 facts。
- `RespawnGameExample` 是目前最完整的示範定義。

## 目前進度
- ✅ 已完成遊戲流程核心、hooks facade、server tick / interact mixin、整合測試。
- ✅ `runGameFeatureTests` 已通過，驗證 object / team / instance flow；相關系統的主線文件已分流。
- ✅ `GameObjectKind` 包含 `RESPAWN_POINT` / `MINEABLE` / `USABLE` / `ATTACKABLE` / `INTERACTABLE` / `CUSTOM`。
- 🟡 目前 mixin 已接到方塊互動與攻擊互動；access 類系統與 client settings UI 仍在持續補齊。

## Public class navigation list
- [GameCore](GameCore.md)
- [GameFeatures](GameFeatures.md)
- [GameBootstrapConfig](GameBootstrapConfig.md)
- [GameObjectConfig](GameObjectConfig.md)
- [GameObjectKind](GameObjectKind.md)
- [GameObjectContext](GameObjectContext.md)
- [GameObjectRuntime](GameObjectRuntime.md)
- [GameObjectDefinition](GameObjectDefinition.md)
- [GameObjectHooks](GameObjectHooks.md)
- [GameTeamColor](GameTeamColor.md)
- [GameTeamDefinition](GameTeamDefinition.md)
- [GameTeamFeature](GameTeamFeature.md)
- [RespawnGameExample](RespawnGameExample.md)
- [GameStateContracts](GameStateContracts.md)
- [GameInstance](GameInstance.md)

## Large demo
```java
Game.init();

GameManager.register(new RespawnGameExample());

GameInstance<?> instance = GameManager.createInstance(
    Identifier.of("myulib", "respawn_game"),
    new GameBootstrapConfig(
        java.util.Map.of(
            Identifier.of("myulib", "respawn_anchor"),
            new GameObjectConfig(
                Identifier.of("myulib", "respawn_anchor"),
                GameObjectKind.RESPAWN_POINT,
                Identifier.of("minecraft", "block"),
                "Respawn Anchor",
                true
            )
        ),
        java.util.Map.of("mode", "demo")
    )
);

instance.teams().register(new GameTeamDefinition(
    Identifier.of("myulib", "alpha"),
    "Alpha",
    GameTeamColor.BLUE,
    true,
    true,
    java.util.Map.of()
));

GameObjectHooks.attach(instance, Identifier.of("myulib", "respawn_anchor"), new Object());
GameObjectHooks.tick(instance);
instance.transition(RespawnGameExample.RespawnGameState.COUNTDOWN);
```

## Reading order
1. `GameCore.md`
2. `GameStateContracts.md`
3. `GameInstance.md`
4. `GameObjectKind.md`
5. `GameObjectContext.md`
6. `GameObjectRuntime.md`
7. `GameObjectDefinition.md`
8. `GameObjectHooks.md`
9. `GameTeamColor.md`
10. `GameTeamDefinition.md`
11. `GameTeamFeature.md`
12. Then the config/example pages
