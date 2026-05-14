package com.myudog.myulib.api.core.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Tile {
    public int id;
    public double weight;

    public Map<Direction, Set<Integer>> adjacency;

    public Tile(int id) {
        this.id = id;
        this.weight = 0;
        this.adjacency = new HashMap<>();
    }
}
