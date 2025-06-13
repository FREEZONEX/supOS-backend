package com.supos.uns.service;

import java.util.*;
import java.util.stream.Collectors;

public class DependencySorter {
    static class Pair<K, V> {
        K key;
        V value;

        Pair(K k, V v) {
            key = k;
            value = v;
        }

        K getKey() {
            return key;
        }

        V getValue() {
            return value;
        }
    }

    public static void main(String[] args) {
        List<Pair<String, String>> dependencies = Arrays.asList(
                new Pair<>("a", "b"),
                new Pair<>("b", "c"),
                new Pair<>("e", "d"),
                new Pair<>("d", "b")
        );

        Map<String, List<String>> reverseGraph = buildReverseGraph(dependencies);
        Map<String, Integer> levelMap = calculateLevels(reverseGraph);
        List<String> sorted = sortNodes(levelMap);

        System.out.println("层级排序结果: " + sorted);
        // 输出: [a, e, d, b, c]
    }

    // 构建反向依赖图
    static Map<String, List<String>> buildReverseGraph(List<Pair<String, String>> deps) {
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> nodes = new HashSet<>();

        for (Pair<String, String> dep : deps) {
            String from = dep.getKey();
            String to = dep.getValue();
            nodes.add(from);
            nodes.add(to);
            graph.computeIfAbsent(to, k -> new ArrayList<>()).add(from);
        }

        // 确保所有节点都在图中
        nodes.forEach(n -> graph.putIfAbsent(n, new ArrayList<>()));
        return graph;
    }

    // 计算层级（记忆化递归）
    static Map<String, Integer> calculateLevels(Map<String, List<String>> graph) {
        Map<String, Integer> levels = new HashMap<>();
        graph.keySet().forEach(n -> getLevel(n, graph, levels));
        return levels;
    }

    private static int getLevel(String node, Map<String, List<String>> graph, Map<String, Integer> levels) {
        if (!levels.containsKey(node)) {
            List<String> parents = graph.get(node);
            int maxLevel = parents.stream()
                    .mapToInt(p -> getLevel(p, graph, levels))
                    .max()
                    .orElse(-1);
            levels.put(node, maxLevel + 1);
        }
        return levels.get(node);
    }

    // 按层级和字母排序
    static List<String> sortNodes(Map<String, Integer> levelMap) {
        return levelMap.entrySet().stream()
                .sorted((e1, e2) -> {
                    int levelCompare = Integer.compare(e1.getValue(), e2.getValue());
                    return levelCompare != 0 ? levelCompare : e1.getKey().compareTo(e2.getKey());
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
