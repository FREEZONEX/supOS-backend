package com.supos.uns.util;

import com.supos.common.utils.PathUtil;
import com.supos.uns.dao.po.UnsPo;

import java.util.*;
import java.util.function.Function;

public class LayRecUtils {

    public static class SaveOrUpdate {
        public final Collection<UnsPo> insertList;
        public final Collection<UnsPo> updateList;

        public SaveOrUpdate(Collection<UnsPo> insertList, Collection<UnsPo> updateList) {
            this.insertList = insertList;
            this.updateList = updateList;
        }

        @Override
        public String toString() {
            return String.format("{insert=%s, update=%s}", insertList, updateList);
        }
    }

    public static SaveOrUpdate setLayRecAndPath(HashMap<Long, UnsPo> addFiles, HashMap<Long, UnsPo> dbFiles) {
        // 合并所有节点（新增覆盖已有）
        Map<Long, UnsPo> allNodes = new TreeMap<>(dbFiles);
        allNodes.putAll(addFiles);

        LinkedList<UnsPo> rootNodes = new LinkedList<>();
        HashMap<Long, List<UnsPo>> childrenMap = new HashMap<>(addFiles.size());
        for (UnsPo node : allNodes.values()) {
            Long pid = node.getParentId();
            if (pid != null) {
                childrenMap.computeIfAbsent(pid, k -> new LinkedList<>()).add(node);
            } else {
                if (node.getPath() == null && node.getName() != null) {
                    node.setPath(node.getName());
                }
                rootNodes.add(node);
            }
        }


        // 需要更新的节点集合
        HashSet<UnsPo> nodesToInsert = new HashSet<>();
        HashSet<UnsPo> nodesToUpdate = new HashSet<>();

        processPathName(rootNodes, addFiles, nodesToUpdate);
        for (List<UnsPo> children : childrenMap.values()) {
            processPathName(children, addFiles, nodesToUpdate);
        }

        final Date updateTime = new Date();
        final Function<UnsPo, Boolean> recorder = po -> {
            Long id = po.getId();
            UnsPo dbPo = dbFiles.get(id);
            if (dbPo == null) {
                return nodesToInsert.add(po);
            } else
//                if (!Objects.equals(po.getParentId(), dbPo.getParentId()) || !Objects.equals(po.getName(), dbPo.getName()))
            {
                po.setUpdateAt(updateTime);
                return nodesToUpdate.add(po);
            }
//            return false;
        };
        for (UnsPo node : allNodes.values()) {
            Long id = node.getId();
            UnsPo dbPo;
            // 判断是否是新增节点或父节点变更的已有节点
            if (addFiles.containsKey(id) || ((dbPo = dbFiles.get(id)) != null && !Objects.equals(node.getParentId(), dbPo.getParentId()))) {
                // 生成当前节点的层级路径
                generatePath(node, allNodes);
                // 收集当前节点及其所有子节点用于更新
                collectAffectedNodes(node, childrenMap, allNodes, recorder);
            }
        }
        return new SaveOrUpdate(nodesToInsert, nodesToUpdate);
    }

    // 生成单个节点的层级路径（递归向上查找）
    private static void generatePath(UnsPo node, Map<Long, UnsPo> allNodes) {
        if (node.getParentId() == null) { // 根节点
            node.setLayRec(String.valueOf(node.getId()));
            node.setPath(node.getPathName());
        } else {
            UnsPo parent = allNodes.get(node.getParentId());
            if (parent == null) { // 处理异常情况
                node.setLayRec(String.valueOf(node.getId()));
                node.setPath(node.getName());
                return;
            }

            // 递归生成父节点路径
            if (parent.getLayRec() == null) {
                generatePath(parent, allNodes);
            }

            node.setLayRec(parent.getLayRec() + "/" + node.getId());
            node.setPath(parent.getPath() + "/" + node.getPathName());
        }
    }

    // 收集受影响的所有子节点（BFS遍历）
    private static void collectAffectedNodes(UnsPo changedNode, Map<Long, List<UnsPo>> childrenMap, Map<Long, UnsPo> allNodes, Function<UnsPo, Boolean> result) {
        Queue<UnsPo> queue = new LinkedList<>();
        queue.offer(changedNode);
        result.apply(changedNode);

        while (!queue.isEmpty()) {
            UnsPo current = queue.poll();
            // 查找所有子节点
            List<UnsPo> children = childrenMap.get(current.getId());
            if (children != null) {
                for (UnsPo node : children) {
                    if (result.apply(node)) { // 避免重复处理
                        queue.offer(node);
                        // 重新生成子节点路径
                        generatePath(node, allNodes);
                    }
                }
            }
        }
    }

    private static void processPathName(List<UnsPo> siblings, HashMap<Long, UnsPo> addFiles, HashSet<UnsPo> nodesToUpdate) {
        if (siblings == null || siblings.isEmpty()) return;

        // 按名称分组，处理同名兄弟节点
        Map<String, List<UnsPo>> nameGroup = new HashMap<>();
        for (UnsPo node : siblings) {
            nameGroup.computeIfAbsent(escapeName(node.getName()), k -> new ArrayList<>()).add(node);
        }

        // 对每个分组按ID排序并设置pathName
        for (Map.Entry<String, List<UnsPo>> entry : nameGroup.entrySet()) {
            String pathName = entry.getKey();
            List<UnsPo> group = entry.getValue();
            if (group.size() == 1) {
                group.get(0).setPathName(pathName);
            } else {
                group.sort(Comparator.comparingLong(UnsPo::getId));
                for (int i = 0; i < group.size(); i++) {
                    UnsPo node = group.get(i);
                    node.setPathName(i > 0 ? pathName + "-" + i : pathName);
                    if (!addFiles.containsKey(node.getId()) && !Objects.equals(PathUtil.getName(node.getPath()), node.getPathName())) {
                        String pDir = PathUtil.subParentPath(node.getPath());
                        String path = pDir != null ? pDir + "/" + node.getPathName() : node.getPathName();
                        node.setPath(path);
                        nodesToUpdate.add(node);
                    }
                }
            }
        }
    }

    private static String escapeName(String name) {
        char[] cs = name.toCharArray();
        boolean changed = false;
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            if ((!Character.isJavaIdentifierPart(c)  && c != '-') || c == '$') {
                changed = true;
                cs[i] = '_';
            }
        }
        return changed ? new String(cs) : name;
    }
}
