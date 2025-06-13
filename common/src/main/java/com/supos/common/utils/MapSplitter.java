package com.supos.common.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapSplitter {

    public static <K,V> List<Map<K, V>> splitMap(Map<K, V> originalMap, int chunkSize) {
        List<Map<K, V>> result = new ArrayList<>();
        if (originalMap == null || originalMap.isEmpty()) {
            return result; // 空 Map 返回空 List
        }

        Map<K, V> currentChunk = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : originalMap.entrySet()) {
            currentChunk.put(entry.getKey(), entry.getValue());
            
            // 每满 chunkSize 条时，提交当前块并重置
            if (currentChunk.size() == chunkSize) {
                result.add(currentChunk);
                currentChunk = new LinkedHashMap<>();
            }
        }

        // 添加最后不满 chunkSize 的剩余数据
        if (!currentChunk.isEmpty()) {
            result.add(currentChunk);
        }
        return result;
    }
}
