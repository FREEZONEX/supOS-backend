package com.supos.adpter.nodered;

import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.nodered.service.NodeRedAdapterService;
import com.supos.adpter.nodered.service.SourceflowAdapterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class SourceflowProxyController {

    @Autowired
    private SourceflowAdapterService nodeRedAdapterService;

    /**
     * 通过此代理方法，实现请求nodeRed /flows接口，只返回当前流程ID的数据，ID从cookie中获取；
     * 如果cookie中不包含ID，则返回空数组
     * @param request
     * @return
     */
    @GetMapping({"/service-api/supos/proxy/flows"})
    @ResponseBody
    public JSONObject proxyGetFlows(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        log.info("==>flows proxy in: cookies={}", cookies);
        String cookieId = "";
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("sup_flow_id".equals(cookie.getName())) {
                    cookieId = cookie.getValue();
                    break;
                }
            }
        }
        if (StringUtils.hasText(cookieId)) {
            return nodeRedAdapterService.proxyGetFlow(Long.parseLong(cookieId));
        } else {
            return nodeRedAdapterService.getFromNodeRed();
        }
    }


}
