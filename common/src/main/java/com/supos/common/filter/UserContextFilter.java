package com.supos.common.filter;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.ObjectUtil;
import com.supos.common.utils.ServletUtil;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.Constants;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Component
public class UserContextFilter implements Filter {

    @Resource
    private TimedCache<String, JSONObject> tokenCache;

    @Resource
    private TimedCache<String, UserInfoVo> userInfoCache;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        try {
            HttpServletRequest request = (HttpServletRequest)servletRequest;
            Cookie cookie = ServletUtil.getCookie(request, Constants.ACCESS_TOKEN_KEY);
            if (ObjectUtil.isNotNull(cookie)){
                JSONObject tokenObj = tokenCache.get(cookie.getValue());
                if (null != tokenObj) {
                    String accessToken = tokenObj.getString("access_token");
                    JWT jwt = JWT.of(accessToken);
                    String sub = jwt.getPayloads().getStr("sub");
                    UserInfoVo userInfoVo = userInfoCache.get(sub);
                    UserContext.set(userInfoVo);
                    log.debug("set user content success!");
                }
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
}