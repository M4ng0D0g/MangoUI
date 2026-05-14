package com.myudog.myulib.api.core.generator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Cell {
    public int x, y, z;
    public Set<Tile> possibleTiles;
    public boolean isCollapsed;

    public Cell(int x, int y, int z, List<Tile> allTiles) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.possibleTiles = new HashSet<Tile>(allTiles);
    }

    public double getEntropy() {
        if (isCollapsed) return Double.MAX_VALUE;
        double sumWeights = 0;
        double sumWeightLogWeight = 0;
        for (Tile tile : possibleTiles) {
            sumWeights += tile.weight;
            sumWeightLogWeight += tile.weight * Math.log(tile.weight);
        }
        return Math.log(sumWeights) - (sumWeightLogWeight / sumWeights);
    }
}
