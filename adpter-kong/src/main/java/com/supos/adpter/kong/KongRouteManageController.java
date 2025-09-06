package com.supos.adpter.kong;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.kong.service.KongAdapterService;
import com.supos.adpter.kong.service.UserMenuService;
import com.supos.adpter.kong.vo.MarkRouteRequestVO;
import com.supos.adpter.kong.vo.ResultVO;
import com.supos.adpter.kong.vo.RouteVO;
import com.supos.common.dto.UserMenuDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class KongRouteManageController {

    @Autowired
    private KongAdapterService KongAdapterService;
    @Autowired
    private UserMenuService userMenuService;

    @Value("${node-red.host:nodered}")
    private String nodeRedHost;
    @Value("${node-red.port:1880}")
    private String nodeRedPort;

    /**
     * 获取所有路由， 并且标记是否需要展示
     * @return
     */
    @GetMapping("/inter-api/supos/kong/routes")
    public ResultVO<List> queryRoutes() {
        List<RouteVO> routes = KongAdapterService.queryRoutes();
        return ResultVO.success(routes);
    }

    @PostMapping("/inter-api/supos/kong/routes")
    public ResultVO markRoutes(@RequestBody List<MarkRouteRequestVO> menus) {
        try {
            KongAdapterService.markMenu(menus);
        } catch (IOException e) {
            log.error("save failed", e);
            return ResultVO.fail("save failed!");
        }
        return ResultVO.success("ok");
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

    /**
     * 获取用户的路由列表
     * @return
     */
    @GetMapping("/inter-api/supos/kong/user/routes")
    public ResultVO<List<RouteVO>> userRoutes() {
        List<RouteVO> routes = userMenuService.getUserRouteList();
        return ResultVO.success(routes);
    }

    /**
     * 用户菜单勾选
     * @param menus
     * @return
     */
    @PostMapping("/inter-api/supos/kong/user/routes/mark")
    public ResultVO markMenu(@RequestBody List<UserMenuDto> menus) {
        return userMenuService.setUserMenu(menus);
    }


    /**
     * kong插件启用状态代理
     */
//    @PatchMapping("/service-api/supos/proxy/kong/plugins/{id}")
//    public ResponseEntity pluginsProxy(@PathVariable("id") String id, @RequestBody JSONObject params) {
//        return KongAdapterService.pluginsProxy(id,params);
//    }

    @GetMapping("/inter-api/supos/kong/resource")
    public ResultVO saveResourceByRoutes(){
        userMenuService.saveResourceByRoutes();
        return ResultVO.success("ok");
    }
}
