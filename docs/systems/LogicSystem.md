# Logic 系統使用說明（入口索引）

`logic/` 是玩法規則引擎層，負責接收 signal、檢查條件、執行動作，並與 `GameInstance`、`TimerManager`、`FieldManager`、`ComponentManager` 以及 access / permission 系統串接。

## 系統入口
- [LogicCore](../api/logic/LogicCore.md)
- [LogicConditions](../api/logic/LogicConditions.md)
- [LogicActions](../api/logic/LogicActions.md)
- [LogicSignals](../api/logic/LogicSignals.md)
- [LogicFactsResolver](../api/logic/LogicFactsResolver.md)

## 公開型別
- `LogicContracts.LogicSignal`
- `LogicContracts.LogicContext<S>`
- `LogicContracts.LogicCondition<S>`
- `LogicContracts.LogicAction<S>`
- `LogicContracts.LogicRule<S>`
- `LogicContracts.LogicRuleSet<S>`
- `LogicContracts.LogicEventBus<S>`
- `LogicEngine<S>`
- `LogicFactsResolver`
- `LogicSignals.*`

## 本系統適合做什麼
- 規則式玩法編排
- timer 完成後切 state
- field 進出檢查與 permission 攔截後執行動作
- role group / component / entity / block signal 的高階處理

## 文件導覽
- 詳細 API：`docs/api/logic/*.md`
