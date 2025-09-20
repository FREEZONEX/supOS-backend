package com.supos.i18n.common;

import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化常量
 * @date 2025/9/2 13:39
 */
public class Constants {

    public static final Set<String> BUILT_IN_LANGUAGE_CODE = Set.of("en_US", "zh_CN");
    public static final String DEFAULT_MODULE_CODE = "platform";

    public static final Map<String, String> LANGUAGE_MAP = Map.of(
            "en", "en_US",
            "zh", "zh_CN"
    );
}
