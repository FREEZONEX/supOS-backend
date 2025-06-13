package com.supos.common.utils;

import cn.hutool.extra.spring.SpringUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URL;

public class RuntimeUtil {

    /**
     * 判断是否本地开发环境
     *
     * @return 是否本地开发环境
     */
    public static boolean isLocalRuntime() {
        URL url = RuntimeUtil.class.getProtectionDomain().getCodeSource().getLocation();
        String filePath = url.getFile();
        return filePath != null ? new File(filePath).isDirectory() : false;
    }

    /**
     * 是否本地调试环境
     * @return
     */
    public static boolean isLocalProfile() {
        return StringUtils.isNotBlank(SpringUtil.getActiveProfile());
    }
}
