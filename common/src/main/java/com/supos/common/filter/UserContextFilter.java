package com.supos.common.filter;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.Constants;
import com.supos.common.dto.BaseResult;
import com.supos.common.event.InitTopicsEvent;
import com.supos.common.service.IPersonConfigService;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.ServletUtil;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.PersonConfigVo;
import com.supos.common.vo.UserInfoVo;
import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.supos.common.Constants.readOnlyMode;

@Slf4j
@Component
public class UserContextFilter implements Filter {

    @Resource
    private TimedCache<String, JSONObject> tokenCache;

    @Resource
    private TimedCache<String, UserInfoVo> userInfoCache;
    @Autowired
    private IPersonConfigService personConfigService;

    @Value("${SYS_OS_LANG:zh-CN}")
    private String systemLocale;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException{
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (readOnlyMode.get() && !"GET".equalsIgnoreCase(request.getMethod())) {
            servletResponse.setContentType("application/json");
            BaseResult msg = new BaseResult();
            msg.setCode(400);
            msg.setMsg("starting... readonly!");
            servletResponse.getWriter().println(JsonUtil.toJson(msg));
            return;
        }
        try {
            Cookie cookie = ServletUtil.getCookie(request, Constants.ACCESS_TOKEN_KEY);
            if (ObjectUtil.isNotNull(cookie)) {
                JSONObject tokenObj = tokenCache.get(cookie.getValue());
                if (null != tokenObj) {
                    String accessToken = tokenObj.getString("access_token");
                    JWT jwt = JWT.of(accessToken);
                    String sub = jwt.getPayloads().getStr("sub");
                    UserInfoVo userInfoVo = userInfoCache.get(sub);

                    PersonConfigVo personConfig = personConfigService.getByUserId(sub);
                    if (personConfig != null) {
                        userInfoVo.setMainLanguage(personConfig.getMainLanguage());
                    } else {
                        userInfoVo.setMainLanguage(systemLocale);
                    }
                    UserContext.set(userInfoVo);
                    log.debug("set user content success!");
                }/*else{
                    UserInfoVo userInfoVo = new UserInfoVo();
                    userInfoVo.setSub("66b5114b-0083-48aa-860a-06f1c06ce4c4");
                    UserContext.set(userInfoVo);
                }*/
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (ClientAbortException ignore) {
            // 客户端断开连接，无需处理
        } catch (Exception e) {
            log.error("UserContextFilter Exception", e);
        } finally {
            UserContext.clear();
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

    @EventListener(classes = InitTopicsEvent.class)
    @Order
    void initFished() {
        readOnlyMode.set(false);
    }
}