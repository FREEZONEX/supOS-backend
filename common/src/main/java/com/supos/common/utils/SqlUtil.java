package com.supos.common.utils;

import cn.hutool.core.util.StrUtil;

public class SqlUtil {

    /**
     * 转义用于 SQL LIKE 查询的关键词
     * @param keyword 原始关键词
     * @return 转义后可用于 LIKE 的关键词（例如 %abc\_123%）
     */
    public static String escapeForLike(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return keyword;
        }
        return keyword
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

}
