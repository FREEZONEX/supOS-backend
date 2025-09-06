package com.supos.uns.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * 接口统一调用 AK/SK调用
 * ClassName ：SuposApi
 * Description：
 * Date：2021/3/17 13:46
 * Author：xinwangji@supos.com
 */
@Slf4j
public class SuposApi {


    private static final String ak = "3ba8a60b48185a9e23d38e962b415603";
    private static final String sk = "335e481694545a53e7385db62bab8bd9";
    public static final String AUTHORIZATION_HEADER = "Authorization";



    /**
     * 获取AKSK签名头
     *
     * @param requestBody
     *            请求参数体
     * @param method
     *            请求方法
     * @param apiUrl
     *            接口地址
     * @return headerMap
     */
    public static Map<String, String> getSignatureHeader(String requestBody, Method method, String apiUrl) {

        Map<String, String> headers = MapUtil.newHashMap();
        // 签名源
        StringBuilder sb = new StringBuilder();
        // HttpMethod
        sb.append(method).append("\n");
        // HTTP URI
        sb.append(apiUrl).append("\n");

        // HTTPContentType
        sb.append(ContentType.JSON).append("\n");
        // CanonicalQueryString
        if (Method.GET.equals(method) || Method.DELETE.equals(method)) {

            Map<String, Object> queryMap = JSONUtil.parseObj(requestBody,true);
            String queryString = beanToQueryString(queryMap, false);
            queryString = getSortQueryStr(queryString);
            sb.append(queryString);
        }
        sb.append("\n");
        // CustomHeaders 自定义头 直接换行
        sb.append("\n");
        log.debug(">>>>>>>>>>>>> AK/SK APPKEY : " + ak + "  SECRET_KEY : " + sk
            + " <<<<<<<<<<<<<<<<");
        log.debug(">>>>>>>>>>>>> AK/SK 签名源内容：\n " + sb + " \n<<<<<<<<<<<<<<<<");
        String signature = DigestUtil.hmac(HmacAlgorithm.HmacSHA256,sk.getBytes(StandardCharsets.UTF_8)).digestHex(sb.toString());
        String finalSignature = "Sign " + ak + "-" + signature;
        headers.put(AUTHORIZATION_HEADER, finalSignature);
        headers.put(Header.CONTENT_TYPE.toString(), ContentType.JSON.toString());
        headers.put("Accept", "application/json");
        log.debug(">>>>>>>>>>>>> AK/SK 签名结果：" + finalSignature + " <<<<<<<<<<<<<<<<");
        // TODO CanonicalCustomHeaders 即HTTP协议头中所有“自定义”的Header。 以X-MC-为前缀的请求头。
        return headers;
    }

    /**
     * 获取查询参数 将URL的queryString部分按照IAM规范转成全小写并且按ACSII小写排序
     *
     * @param apiPath
     *            apiPath
     * @return 排序好的查询参数
     */
    private static String getSortQueryStr(String apiPath) {
        if (StrUtil.isBlank(apiPath)) {
            return "";
        }
        Map<String, Object> map = getUrlParams(apiPath);
        TreeMap<String, Object> queryKvMap = new TreeMap<>();

        if (MapUtil.isEmpty(map)) {
            return "";
        }
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = (Map.Entry)iterator.next();
            String key = entry.getKey();
            Object value = entry.getValue();
            queryKvMap.put(key.toLowerCase(Locale.ROOT), value);
        }
        return getUrlParamsByMap(queryKvMap);
    }

    /**
     * 将map转换成url
     */
    public static String getUrlParamsByMap(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue());
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = StrUtil.subBefore(s, "&", true);
        }
        return s;
    }

    /**
     * URL转MAP
     */
    private static Map<String, Object> getUrlParams(String url) {
        Map<String, Object> map = new HashMap<>(0);
        String[] params = url.split("&");
        for (String param : params) {
            String[] p = param.split("=");
            if (p.length == 2) {
                map.put(p[0], p[1]);
            }
        }
        return map;
    }

    private static String beanToQueryString(Map<String, Object> paramMap, boolean isEncoder) {
        if (MapUtil.isEmpty(paramMap)) {
            return null;
        }
        if (isEncoder) {
            return HttpUtil.toParams(paramMap);
        }
        StringBuilder sb = new StringBuilder();
        Iterator iterator = paramMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = (Map.Entry)iterator.next();
            String key = entry.getKey();
            Object value = entry.getValue();
            sb.append(key).append("=").append(value).append("&");
        }
        return sb.substring(0, sb.length() - 1);
    }
}