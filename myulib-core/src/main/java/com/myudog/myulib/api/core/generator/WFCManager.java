package com.myudog.myulib.api.core.generator;

import java.util.*;
import java.util.stream.Collectors;

public class WFCManager {

    private Cell[][][] grid;
    private final int width, height, depth;
    private final List<Tile> allTiles;
    private final Map<Integer, Tile> idToTileMap;
    private final Stack<GridState> history = new Stack<>();
    private final Random random;

    public WFCManager(Cell[][][] existingGrid, List<Tile> allTiles, long seed) {
        this.grid = existingGrid;
        this.width = existingGrid.length;
        this.height = existingGrid[0].length;
        this.depth = existingGrid[0][0].length;
        this.allTiles = allTiles;
        this.random = new Random(seed);

        this.idToTileMap = new HashMap<>();
        for (Tile t : allTiles) {
            idToTileMap.put(t.id, t);
        }
    }

    public void runWFCWithBacktracking() {
        while (true) {
            Cell nextCell = findLowestEntropyCell();
            if (nextCell == null) break; // 完成生成

            // 1. 存檔：紀錄目前的網格狀態
            GridState snapshot = new GridState(grid, nextCell);

            // 2. 嘗試塌陷
            try {
                snapshot.attemptedTile = collapseCell(nextCell); // 紀錄剛試過的 Tile
                history.push(snapshot);

                propagate(nextCell);
            }
            catch (RuntimeException e) {
                undoLastStep();
            }
        }
    }

    private Cell findLowestEntropyCell() {
        Cell bestCell = null;
        double minEntropy = Double.MAX_VALUE;

        for (int w = 0; w < width; ++w) {
            for (int h = 0; h < height; ++h) {
                for (int d = 0; d < depth; ++d) {
                    Cell cell = grid[w][h][d];
                    if (cell.isCollapsed || cell.possibleTiles.size() <= 1) continue;

                    double entropy = cell.getEntropy();
                    double noise = random.nextDouble() * 0.0001;
                    if (entropy + noise < minEntropy) {
                        minEntropy = entropy + noise;
                        bestCell = cell;
                    }
                }
            }
        }
        return bestCell;
    }

    private void propagate(Cell startCell) {
        Stack<Cell> stack = new Stack<>();
        stack.push(startCell);

        while(!stack.isEmpty()) {
            Cell current = stack.pop();

            for (Direction dir : Direction.values()) {
                int nx = current.x + dir.dx;
                int ny = current.y + dir.dy;
                int nz = current.z + dir.dz;
                if (!isValid(nx, ny, nz)) continue;

                Cell neighbor = grid[nx][ny][nz];
                if (neighbor.isCollapsed) continue;

                boolean changed = reduceNeighborPossibilities(current, neighbor, dir);
                if (changed) {
                    stack.push(neighbor);
                    if (neighbor.possibleTiles.isEmpty()) {
                        throw new RuntimeException("生成失敗：發生矛盾於 " + nx + "," + ny + "," + nz);
                    }
                }

            }
        }

    }

    private boolean isValid(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0) return false;
        return x < width && y < height && z < depth;
    }

    private boolean reduceNeighborPossibilities(Cell current, Cell neighbor, Direction dir) {
        Set<Integer> allAllowedByCurrent = new HashSet<>();

        // 收集 current 剩餘所有 Tile 在 dir 方向上允許的所有鄰居 ID
        for (Tile t : current.possibleTiles) {
            allAllowedByCurrent.addAll(t.adjacency.get(dir));
        }

        // 檢查 neighbor 現有的候選名單，移除不在 allAllowedByCurrent 內的 Tile
        Iterator<Tile> iterator = neighbor.possibleTiles.iterator();
        boolean changed = false;

        while (iterator.hasNext()) {
            Tile neighborTile = iterator.next();
            if (!allAllowedByCurrent.contains(neighborTile.id)) {
                iterator.remove();
                changed = true;
            }
        }

        return changed;
    }

    private Tile collapseCell(Cell cell) {
        // 使用注入的 random.nextDouble() 替換 Math.random()
        double totalWeight = cell.possibleTiles.stream().mapToDouble(t -> t.weight).sum();
        double randomValue = random.nextDouble() * totalWeight;

        double cursor = 0;
        Tile selectedTile = null;
        for (Tile tile : cell.possibleTiles) {
            cursor += tile.weight;
            if (randomValue <= cursor) {
                selectedTile = tile;
                break;
            }
        }

        cell.possibleTiles.clear();
        cell.possibleTiles.add(selectedTile);
        cell.isCollapsed = true;
        return selectedTile;
    }

    private void undoLastStep() {
        if (history.isEmpty()) throw new RuntimeException("地圖無解：回溯已至起點");

        GridState lastState = history.pop();

        // 恢復網格狀態
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    // 修正點：將 Set<Integer> 轉換回 Set<Tile>

                    grid[x][y][z].possibleTiles = lastState.cellStateSnapshot[x][y][z].stream()
                            .map(idToTileMap::get)
                            .collect(Collectors.toSet());
                    grid[x][y][z].isCollapsed = lastState.isCollapsedSnapshot[x][y][z];
                }
            }
        }

        // 從剛才出錯的那一格中，移除導致失敗的那個 Tile
        Cell target = grid[lastState.collapsedCell.x][lastState.collapsedCell.y][lastState.collapsedCell.z];
        target.possibleTiles.remove(lastState.attemptedTile);

        if (target.possibleTiles.isEmpty()) {
            undoLastStep(); // 連鎖回溯
        } else {
            propagate(target);
        }
    }



}
