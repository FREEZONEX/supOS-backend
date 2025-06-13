package com.supos.uns.util;

import java.util.ArrayList;
import java.util.List;

public class LongestCommonPrefix {
    public static String longestCommonPrefix(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        // 将每个路径分割为目录数组
        List<String[]> dirsList = new ArrayList<>();
        int minDirs = Integer.MAX_VALUE;
        for (String s : strs) {
            String[] dirs = s.split("/");
            dirsList.add(dirs);
            minDirs = Math.min(minDirs, dirs.length);
        }

        // 如果存在空路径，则返回空字符串
        if (minDirs == 0) {
            return "";
        }

        int commonLevel = 0;
        // 逐层比较目录层级
        for (; commonLevel < minDirs; commonLevel++) {
            String currentLevel = dirsList.get(0)[commonLevel];
            boolean allMatch = true;
            for (int j = 1; j < dirsList.size(); j++) {
                if (!currentLevel.equals(dirsList.get(j)[commonLevel])) {
                    allMatch = false;
                    break;
                }
            }
            if (!allMatch) {
                break;
            }
        }

        if (commonLevel == 0) {
            return "";
        }

        // 构建公共前缀路径
        StringBuilder prefix = new StringBuilder();
        String[] firstDirs = dirsList.get(0);
        for (int i = 0; i < commonLevel; i++) {
            prefix.append(firstDirs[i]).append('/');
        }
        return prefix.toString();
    }

}
