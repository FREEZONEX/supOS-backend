package com.supos.uns.util;

import java.util.*;
import java.util.function.Function;

public class DependencySorter {

    // 计算层级（记忆化递归）
    public static <R> Map<R, Integer> calculateLevels(Map<R, List<R>> graph) {
        Map<R, Integer> levels = new HashMap<>();
        graph.keySet().forEach(n -> getLevel(n, graph, levels));
        return levels;
    }

    // 构建反向依赖图
    public static <T, R> Map<R, List<R>> buildReverseGraph(List<T> deps, Function<T, R> k, Function<T, R> v) {
        HashMap<R, List<R>> graph = new HashMap<>();
        HashSet<R> nodes = new HashSet<>();

        for (T dep : deps) {
            R from = k.apply(dep);
            R to = v.apply(dep);
            nodes.add(from);
            nodes.add(to);
            graph.computeIfAbsent(to, rr -> new ArrayList<>()).add(from);
        }

        // 确保所有节点都在图中
        nodes.forEach(n -> graph.putIfAbsent(n, new ArrayList<>()));
        return graph;
    }

    private static <R> int getLevel(R node, Map<R, List<R>> graph, Map<R, Integer> levels) {
        if (!levels.containsKey(node)) {
            List<R> parents = graph.get(node);
            int maxLevel = parents.stream()
                    .mapToInt(p -> getLevel(p, graph, levels))
                    .max()
                    .orElse(-1);
            levels.put(node, maxLevel + 1);
        }
        return levels.get(node);
    }
}
