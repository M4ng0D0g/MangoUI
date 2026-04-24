package com.myudog.myulib.api.game.examples;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.event.ProcessResult;
import com.myudog.myulib.api.event.listener.EventListener;
import com.myudog.myulib.api.game.core.GameConfig;
import com.myudog.myulib.api.game.core.GameData;
import com.myudog.myulib.api.game.core.GameDefinition;
import com.myudog.myulib.api.game.core.GameInstance;
import com.myudog.myulib.api.game.event.GameObjectInteractEvent;
import com.myudog.myulib.api.game.object.IGameObject;
import com.myudog.myulib.api.game.object.impl.BlockGameObject;
import com.myudog.myulib.api.game.object.impl.InteractableObject;
import com.myudog.myulib.api.game.state.BasicGameStateMachine;
import com.myudog.myulib.api.game.state.GameState;
import com.myudog.myulib.api.game.state.GameStateMachine;
import com.myudog.myulib.api.team.TeamColor;
import com.myudog.myulib.api.team.TeamDefinition;
import com.myudog.myulib.internal.event.EventDispatcherImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TicTacToeGameDefinition extends GameDefinition<TicTacToeGameDefinition.TicTacToeConfig, TicTacToeGameDefinition.TicTacToeData, TicTacToeGameDefinition.TicTacToeState> {
    public static final Identifier GAME_ID = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "tictactoe");
    private final TicTacToeBinding binding = new TicTacToeBinding();

    public TicTacToeGameDefinition(Identifier id) {
        super(id);
    }

    @Override
    public TicTacToeData createInitialData(TicTacToeConfig config) {
        return new TicTacToeData();
    }

    @Override
    public GameStateMachine<TicTacToeState> createStateMachine(TicTacToeConfig config) {
        return new BasicGameStateMachine<>(
                TicTacToeState.WAITING,
                Map.of(
                        TicTacToeState.WAITING, Set.of(TicTacToeState.RUNNING),
                        TicTacToeState.RUNNING, Set.of(TicTacToeState.FINISHED)
                )
        );
    }

    @Override
    protected EventDispatcherImpl createEventBus() {
        return new EventDispatcherImpl();
    }

    @Override
    protected void bindBehavior(GameInstance<TicTacToeConfig, TicTacToeData, TicTacToeState> instance) {
        binding.bind(instance);
    }

    @Override
    protected void unbindBehavior(GameInstance<TicTacToeConfig, TicTacToeData, TicTacToeState> instance) {
        binding.unbind(instance);
    }

    public enum TicTacToeState implements GameState {
        WAITING,
        RUNNING,
        FINISHED
    }

    public enum CellState {
        EMPTY,
        RED,
        BLUE
    }

    public static final class TicTacToeData extends GameData {
        private final Map<Identifier, CellState> board = new java.util.LinkedHashMap<>();
        private final List<Identifier> turnOrder = new ArrayList<>();

        public CellState getCell(Identifier id) {
            return board.getOrDefault(id, CellState.EMPTY);
        }

        public void setCell(Identifier id, CellState state) {
            board.put(id, state);
        }

        public List<Identifier> turnOrder() {
            return turnOrder;
        }
    }

    public record TicTacToeConfig(
            Map<Identifier, IGameObject> gameObjects,
            Vec3 start,
            UUID bluePlayerId,
            List<Identifier> cellIds
    ) implements GameConfig {

        public static TicTacToeConfig fromStart(Vec3 start, UUID bluePlayerId) {
            Map<Identifier, IGameObject> objects = new java.util.LinkedHashMap<>();
            List<Identifier> ids = new ArrayList<>();

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    Identifier id = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "tictactoe/cell_" + row + "_" + col);
                    InteractableObject cell = new InteractableObject(id);
                    cell.set(BlockGameObject.POS, new Vec3(start.x + col, start.y, start.z + row));
                    cell.set(BlockGameObject.BLOCK_STATE, Blocks.WHITE_WOOL.defaultBlockState());
                    cell.set(InteractableObject.COOLDOWN_MS, 50L);
                    objects.put(id, cell);
                    ids.add(id);
                }
            }

            return new TicTacToeConfig(
                    Collections.unmodifiableMap(objects),
                    start,
                    bluePlayerId,
                    List.copyOf(ids)
            );
        }

        @Override
        public boolean validate() {
            if (!GameConfig.validateDefinitions(this)) {
                return false;
            }
            if (cellIds == null || cellIds.size() != 9) {
                throw new IllegalArgumentException("井字棋需要剛好 9 個格子");
            }
            if (bluePlayerId == null) {
                throw new IllegalArgumentException("必須指定藍隊玩家");
            }
            return true;
        }

        @Override
        public int maxPlayer() {
            return 2;
        }

        @Override
        public Map<String, Identifier> teams() {
            return Map.of(
                    GameConfig.SPECTATOR_TEAM_KEY, GameConfig.SPECTATOR_TEAM_ID,
                    "blue", Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "tictactoe/blue")
            );
        }

        @Override
        public List<TeamDefinition> additionalTeamDefinitions() {
            return List.of(new TeamDefinition(
                    Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "tictactoe/blue"),
                    Component.literal("TicTacToe Blue"),
                    TeamColor.BLUE,
                    Map.of()
            ));
        }
    }

    private static final class TicTacToeBinding {
        private static final int[][] LINES = new int[][]{
                {0, 1, 2},
                {3, 4, 5},
                {6, 7, 8},
                {0, 3, 6},
                {1, 4, 7},
                {2, 5, 8},
                {0, 4, 8},
                {2, 4, 6}
        };

        private EventListener<GameObjectInteractEvent> interactListener;

        public void bind(GameInstance<TicTacToeConfig, TicTacToeData, TicTacToeState> instance) {
            TicTacToeConfig cfg = instance.getConfig();
            TicTacToeData data = instance.getData();

            for (Identifier cellId : cfg.cellIds()) {
                data.setCell(cellId, CellState.EMPTY);
                data.turnOrder().add(cellId);
                repaintCell(instance, cellId, CellState.EMPTY);
            }

            // 先手紅方先隨機落一子
            playRandomRed(instance);
            instance.transition(TicTacToeState.RUNNING);

            interactListener = event -> {
                if (instance.getCurrentState() != TicTacToeState.RUNNING) {
                    return ProcessResult.PASS;
                }

                ServerPlayer player = event.player();
                if (!player.getUUID().equals(cfg.bluePlayerId())) {
                    player.sendSystemMessage(Component.literal("只有藍隊玩家可操作棋盤"));
                    return ProcessResult.CONSUME;
                }

                Identifier cellId = event.target().getId();
                if (!cfg.cellIds().contains(cellId)) {
                    return ProcessResult.PASS;
                }

                if (data.getCell(cellId) != CellState.EMPTY) {
                    player.sendSystemMessage(Component.literal("該格已被佔用"));
                    return ProcessResult.CONSUME;
                }

                data.setCell(cellId, CellState.BLUE);
                repaintCell(instance, cellId, CellState.BLUE);

                if (isWinner(cfg, data, CellState.BLUE)) {
                    end(instance, "藍方勝利");
                    return ProcessResult.CONSUME;
                }

                if (isDraw(cfg, data)) {
                    end(instance, "平手");
                    return ProcessResult.CONSUME;
                }

                playRandomRed(instance);
                if (isWinner(cfg, data, CellState.RED)) {
                    end(instance, "紅方勝利");
                    return ProcessResult.CONSUME;
                }

                if (isDraw(cfg, data)) {
                    end(instance, "平手");
                }

                return ProcessResult.CONSUME;
            };

            instance.getEventBus().subscribe(GameObjectInteractEvent.class, interactListener);
        }

        public void unbind(GameInstance<TicTacToeConfig, TicTacToeData, TicTacToeState> instance) {
            if (interactListener != null) {
                instance.getEventBus().unsubscribe(GameObjectInteractEvent.class, interactListener);
                interactListener = null;
            }
        }

        private static void playRandomRed(GameInstance<TicTacToeConfig, TicTacToeData, TicTacToeState> instance) {
            TicTacToeConfig cfg = instance.getConfig();
            TicTacToeData data = instance.getData();

            List<Identifier> empty = new ArrayList<>();
            for (Identifier id : cfg.cellIds()) {
                if (data.getCell(id) == CellState.EMPTY) {
                    empty.add(id);
                }
            }

            if (empty.isEmpty()) {
                return;
            }

            Identifier picked = empty.get(ThreadLocalRandom.current().nextInt(empty.size()));
            data.setCell(picked, CellState.RED);
            repaintCell(instance, picked, CellState.RED);
        }

        private static boolean isWinner(TicTacToeConfig cfg, TicTacToeData data, CellState state) {
            List<Identifier> ids = cfg.cellIds();
            for (int[] line : LINES) {
                if (data.getCell(ids.get(line[0])) == state
                        && data.getCell(ids.get(line[1])) == state
                        && data.getCell(ids.get(line[2])) == state) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isDraw(TicTacToeConfig cfg, TicTacToeData data) {
            for (Identifier id : cfg.cellIds()) {
                if (data.getCell(id) == CellState.EMPTY) {
                    return false;
                }
            }
            return true;
        }

        private static void repaintCell(GameInstance<TicTacToeConfig, TicTacToeData, TicTacToeState> instance, Identifier id, CellState state) {
            IGameObject runtime = instance.getData().getObject(id).orElse(null);
            if (!(runtime instanceof InteractableObject cell)) {
                return;
            }

            switch (state) {
                case RED -> cell.set(BlockGameObject.BLOCK_STATE, Blocks.RED_WOOL.defaultBlockState());
                case BLUE -> cell.set(BlockGameObject.BLOCK_STATE, Blocks.BLUE_WOOL.defaultBlockState());
                default -> cell.set(BlockGameObject.BLOCK_STATE, Blocks.WHITE_WOOL.defaultBlockState());
            }

            cell.destroy(instance);
            cell.spawn(instance);
        }

        private static void end(GameInstance<TicTacToeConfig, TicTacToeData, TicTacToeState> instance, String msg) {
            ServerPlayer blue = instance.getLevel().getServer().getPlayerList().getPlayer(instance.getConfig().bluePlayerId());
            if (blue != null) {
                blue.sendSystemMessage(Component.literal("井字棋：" + msg));
            }
            instance.transition(TicTacToeState.FINISHED);
        }
    }
}

