package com.supos.adpter.nodered;

import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.nodered.service.EventflowAdapterService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("EventFlowNodeRedProxyController")
@Slf4j
public class EventflowProxyController {

    @Autowired
    private EventflowAdapterService eventflowAdapterService;

    /**
     * 通过此代理方法，实现请求nodeRed /flows接口，只返回当前流程ID的数据，ID从cookie中获取；
     * 如果cookie中不包含ID，则返回空数组
     * @param request
     * @return
     */
    @GetMapping({"/service-api/supos/proxy/event/flows"})
    @ResponseBody
    public JSONObject proxyGetFlows(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        log.info("==>flows proxy in: cookies={}", cookies);
        String cookieId = "";
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("sup_event_flow_id".equals(cookie.getName())) {
                    cookieId = cookie.getValue();
                    break;
                }
            }
        }
        if (StringUtils.hasText(cookieId)) {
            return eventflowAdapterService.proxyGetFlow(Long.parseLong(cookieId));
        } else {
            return eventflowAdapterService.getFromNodeRed();
        }
    }
}
