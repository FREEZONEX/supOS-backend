package com.supos.adpter.kong;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.supos.adpter.kong.service.KongAdapterService;
import com.supos.adpter.kong.vo.ResultVO;
import com.supos.adpter.kong.vo.SimpleRouteVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class KongRouteManageController {

    @Autowired
    private KongAdapterService KongAdapterService;

    @Value("${node-red.host:nodered}")
    private String nodeRedHost;
    @Value("${node-red.port:1880}")
    private String nodeRedPort;


    @GetMapping("/inter-api/supos/kong/routeList")
    public ResultVO<List<SimpleRouteVo>> routeList() {
        return ResultVO.success(KongAdapterService.routeList());
    }

    /**
     * 通过此代理方法，实现请求nodeRed /flows接口，只返回当前流程ID的数据，ID从cookie中获取；
     * 如果cookie中不包含ID，则返回空数组
     * @param request
     * @return
     */
    @GetMapping({"/test/nodered", "/flows/test/nodered"})
    public String proxyNodeRedFlows(HttpServletRequest request) {
        String flowId = "";
        List<HttpCookie> cookieList = new ArrayList<>();
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if ("flowId".equals(cookie.getName())) {
                flowId = cookie.getValue();
            }
            cookieList.add(new HttpCookie(cookie.getName(), cookie.getValue()));
            log.info("cookie key = {}, value = {}", cookie.getName(), cookie.getValue());
        }
        HttpRequest postClient = HttpUtil.createGet(String.format("http://%s:%s/flows", nodeRedHost, nodeRedPort));
        postClient.cookie(cookieList);
        HttpResponse response = postClient.execute();
        log.info("<=== Get node response: {}", response.body());
        return response.body();
    }
}
