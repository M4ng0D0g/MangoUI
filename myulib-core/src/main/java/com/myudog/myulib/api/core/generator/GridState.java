package com.myudog.myulib.api.core.generator;

import java.util.HashSet;
import java.util.Set;

public class GridState {
    // 改用三維陣列儲存 ID 列表，記憶體佔用更小
    public Set<Integer>[][][] cellStateSnapshot;
    public boolean[][][] isCollapsedSnapshot;
    public Cell collapsedCell;
    public Tile attemptedTile;

    @SuppressWarnings("unchecked")
    public GridState(Cell[][][] grid, Cell current) {
        int w = grid.length;
        int h = grid[0].length;
        int d = grid[0][0].length;
        
        this.collapsedCell = current;
        this.cellStateSnapshot = new HashSet[w][h][d];
        this.isCollapsedSnapshot = new boolean[w][h][d];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    // 只存 ID，減少對象參照
                    Set<Integer> ids = new HashSet<>();
                    for(Tile t : grid[x][y][z].possibleTiles) ids.add(t.id);
                    this.cellStateSnapshot[x][y][z] = ids;
                    this.isCollapsedSnapshot[x][y][z] = grid[x][y][z].isCollapsed;
                }
            }
        }
    }
}