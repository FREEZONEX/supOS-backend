package com.supos.uns.util;

import com.supos.uns.dao.po.UnsPo;

import java.util.*;

public class LeastTopNodeUtil {

    public static List<UnsPo> getLeastTopNodes(List<UnsPo> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();

        // 构建节点映射和子节点树
        Map<Long, UnsPo> nodeMap = new HashMap<>();
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (UnsPo node : list) {
            nodeMap.put(node.getId(), node);
            childrenMap.put(node.getId(), new ArrayList<>());
        }
        for (UnsPo node : list) {
            Long parentId = node.getParentId();
            if (parentId != null && nodeMap.containsKey(parentId)) {
                childrenMap.get(parentId).add(node.getId());
            }
        }

        Set<Long> covered = new HashSet<>();
        ArrayList<UnsPo> result = new ArrayList<>(list.size());

        for (UnsPo node : list) {
            Long currentId = node.getId();
            if (covered.contains(currentId)) continue;

            // 向上查找最顶层的未覆盖祖先
            Long topId = findTopNode(currentId, nodeMap, covered);
            if (topId == null) continue;

            result.add(nodeMap.get(topId));
            markDescendants(topId, childrenMap, covered);
        }

        return result;
    }

    private static Long findTopNode(Long startId, Map<Long, UnsPo> nodeMap, Set<Long> covered) {
        Long currentId = startId;
        Long topId = null;
        boolean isChainUncovered = true;

        while (currentId != null) {
            if (covered.contains(currentId)) {
                isChainUncovered = false;
                break;
            }
            topId = currentId;
            UnsPo current = nodeMap.get(currentId);
            Long parentId = current.getParentId();
            if (parentId == null || !nodeMap.containsKey(parentId)) break;
            currentId = parentId;
        }

        return isChainUncovered ? topId : null;
    }

    private static void markDescendants(Long rootId, Map<Long, List<Long>> childrenMap, Set<Long> covered) {
        Queue<Long> queue = new LinkedList<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            if (covered.contains(currentId)) continue;
            covered.add(currentId);
            queue.addAll(childrenMap.get(currentId));
        }
    }
}
