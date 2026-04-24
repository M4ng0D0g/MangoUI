# GameScopeTokens

`GameScopeTokens` composes scoped identifiers for runtime resources.

## Why

A stable token path prevents id collisions across games and instances.

## Structure

- namespace: `modToken`
- path: `gameToken/definitionToken/instanceToken`

## API

- `scoped(mod, game, def, instance)`
- `scoped(gameDefinition, def, instance)`
- `scopedFromParts(mod, tokens...)`

## Example

```java
Identifier teamId = GameScopeTokens.scoped("myulib", "chess", "white", "room_a");
// myulib:chess/white/room_a
```

