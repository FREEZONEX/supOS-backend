package com.supos.common.utils;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月19日 8:45
 */
@Slf4j
public class FuxaUtils {
    private FuxaUtils() {
    }

    public static String getFuxaUrl() {
        String fuxaUrl;
        if (RuntimeUtil.isLocalProfile()) {
            fuxaUrl = "http://100.100.100.22:33893/fuxa/home";
        } else {
            fuxaUrl = "http://fuxa:1881";
        }
        return fuxaUrl;
    }

    public static boolean create(String dashboardJson) {
        log.debug(">>>>>>>>>>>>>>>fuxa 创建 dashboards 请求:{}", dashboardJson);
        HttpResponse dashboardResponse = HttpUtil.createPost(getFuxaUrl() + "/api/project").body(dashboardJson).execute();
        log.debug(">>>>>>>>>>>>>>>fuxa 创建 dashboards 返回结果:{}", dashboardResponse.body());
        return 200 == dashboardResponse.getStatus();
    }

    public static String get(String layoutId) {
        String url = getFuxaUrl() + "/api/project?layoutId=" + layoutId;
        log.debug(">>>>>>>>>>>>>>>fuxa 查询 dashboards 请求 :{}", url);
        HttpResponse response = HttpUtil.createGet(url).execute();
        log.debug(">>>>>>>>>>>>>>>fuxa 查询 dashboards 返回结果:{}", response.body());
        if (response.getStatus() != 200) {
            return null;
        }
        return response.body();
    }
}
