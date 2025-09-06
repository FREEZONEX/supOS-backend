package com.supos.common.utils;

import com.supos.common.dto.CreateTopicDto;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class LayRecUtil {

    /**
     * 判断 childLayRec 是否是 parentLayRec 的子路径（严格路径匹配）
     * 示例：
     * - contains("a/b/c", "a") => true
     * - contains("a/b/c", "a/b") => true
     * - contains("a/b", "a/b") => true
     * - contains("a/b", "a/bb") => false
     */
    public static boolean contains(String childLayRec, String parentLayRec) {
        if (childLayRec == null || parentLayRec == null) {
            return false;
        }
        if (childLayRec.equals(parentLayRec)) {
            return true;
        }
        return childLayRec.startsWith(parentLayRec + "/");
    }

    public static String getParentLayRec(String layRec) {
        if (layRec == null || !layRec.contains("/")) {
            return null;
        }
        int lastSlashIndex = layRec.lastIndexOf('/');
        return layRec.substring(0, lastSlashIndex);
    }

    /**
     * 构建父路径到其直接及所有子孙节点的映射索引
     * <p>
     * 核心思路：
     * 对于每个节点的完整路径 layRec，拆解出所有的父路径，
     * 然后把该节点添加到每个父路径对应的子节点列表中，
     * 方便后续根据父路径快速查找所有子孙节点，避免重复遍历。
     *
     * @param allNodes 所有节点列表，必须包含 getLayRec() 和 getId()
     * @return Map，key是父路径，value是该父路径下的所有子孙节点列表
     */
    public static Map<String, List<CreateTopicDto>> buildParentToChildrenMap(List<CreateTopicDto> allNodes) {
        Map<String, List<CreateTopicDto>> parentToChildrenMap = new HashMap<>();

        for (CreateTopicDto node : allNodes) {
            String layRec = node.getLayRec();
            if (layRec == null || layRec.isEmpty()) {
                continue; // 跳过空路径
            }
            // 将路径以 "/" 分割，构造所有父路径
            String[] segments = layRec.split("/");
            StringBuilder parentPathBuilder = new StringBuilder();

            // 遍历除了自身的所有父路径片段
            for (int i = 0; i < segments.length - 1; i++) {
                if (i > 0) {
                    parentPathBuilder.append("/");
                }
                parentPathBuilder.append(segments[i]);
                String parentPath = parentPathBuilder.toString();

                // 将当前节点加入对应父路径的子节点列表
                parentToChildrenMap
                        .computeIfAbsent(parentPath, k -> new ArrayList<>())
                        .add(node);
            }
        }
        return parentToChildrenMap;
    }

    public static Map<String, List<CreateTopicDto>> buildParentToChildrenMap2(List<CreateTopicDto> allNodes) {
        long t1 = System.currentTimeMillis();
        Map<String, List<CreateTopicDto>> parentToChildrenMap = new HashMap<>(allNodes.size() * 2);

        for (CreateTopicDto node : allNodes) {
            String layRec = node.getLayRec();
            if (layRec == null || layRec.isEmpty()) {
                continue;
            }

            int index = -1;
            while ((index = layRec.indexOf('/', index + 1)) != -1) {
                // 父路径是 layRec 的前缀
                String parentPath = layRec.substring(0, index);
                parentToChildrenMap
                        .computeIfAbsent(parentPath, k -> new ArrayList<>(4))
                        .add(node);
            }
        }
        log.debug("buildParentToChildrenMap2 耗时：" + (System.currentTimeMillis() - t1));
        return parentToChildrenMap;
    }

    /**
     * 对比父级和当前节点的layRec ，获取父级节点的下一级节点layRec，如父级为空，则返回父级layRec
     * @param basePath 父级layRec
     * @param path 当前节点layRec
     * @return
     */
    public static String getNextNodeAfterBasePath2(String basePath, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        int baseLen = basePath == null ? 0 : basePath.length();

        // 如果 basePath 为空，直接从 path 提取第一个节点
        if (baseLen == 0 || "0".equals(basePath)) {
            int slashIndex = path.indexOf('/');
            return slashIndex == -1 ? path : path.substring(0, slashIndex);
        }

        // path 必须以 basePath 开头
        if (!path.startsWith(basePath)) {
            return null;
        }

        int index = baseLen;

        // 如果 basePath 末尾没有 '/'，但 path 紧接的是 '/'，跳过它
        if (index < path.length() && path.charAt(index) == '/') {
            index++;
        }

        // ✅ 特别处理：basePath 和 path 完全一致，或后面只有一个 '/'
        if (index >= path.length()) {
            return "";
        }

        int nextSlashIndex = path.indexOf('/', index);

        if (nextSlashIndex != -1) {
            return path.substring(index, nextSlashIndex);
        } else {
            return path.substring(index);
        }
    }
}
