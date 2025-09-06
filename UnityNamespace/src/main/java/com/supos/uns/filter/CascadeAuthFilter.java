//package com.supos.uns.filter;
//
//import cn.hutool.cache.impl.TimedCache;
//import cn.hutool.core.util.IdUtil;
//import cn.hutool.http.HttpRequest;
//import cn.hutool.http.HttpResponse;
//import cn.hutool.http.Method;
//import cn.hutool.jwt.JWT;
//import cn.hutool.system.SystemUtil;
//import com.alibaba.fastjson2.JSON;
//import com.alibaba.fastjson2.JSONObject;
//import com.supos.common.Constants;
//import com.supos.common.config.SystemConfig;
//import com.supos.common.dto.auth.AddUserDto;
//import com.supos.common.exception.BuzException;
//import com.supos.common.exception.vo.ResultVO;
//import com.supos.common.utils.KeycloakUtil;
//import com.supos.common.vo.UserInfoVo;
//import com.supos.common.vo.UserManageVo;
//import com.supos.gateway.model.vo.TokenVo;
//import com.supos.gateway.service.AuthService;
//import com.supos.uns.dao.mapper.UserManageMapper;
//import com.supos.uns.service.UserManageService;
//import com.supos.uns.util.SuposApi;
//import jakarta.annotation.Resource;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Map;
//
//@Slf4j
//@Component
//public class CascadeAuthFilter implements Filter {
//
//    @Autowired
//    private UserManageService userManageService;
//    @Autowired
//    private KeycloakUtil keycloakUtil;
//    @Autowired
//    private UserManageMapper userMapper;
//    @Autowired
//    private AuthService authService;
//    @Autowired
//    private TimedCache<String, UserInfoVo> userInfoCache;
//    @Autowired
//    private SystemConfig systemConfig;
//
//    /**
//     * key:supos_community_token
//     * value:token_info json
//     * 默认1小时
//     */
//    @Resource
//    private TimedCache<String, com.alibaba.fastjson.JSONObject> tokenCache;
//
//    private static final String URL_PREFIX = "/inter-api/supos/cascade/auth";
//
//    @Override
//    public void init(FilterConfig filterConfig) throws ServletException {
//        Filter.super.init(filterConfig);
//    }
//
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
//        HttpServletRequest request = (HttpServletRequest) servletRequest;
//        HttpServletResponse httpResponse = (HttpServletResponse) response;
//        String path = request.getRequestURI();
//        if (path.startsWith(URL_PREFIX)) {
//            log.info("cascade SUPOS 单点登录进入path :{}", path);
//            String suposAddress = SystemUtil.get("SUPOS_EE_ADDRESS");
//            Boolean syncUserEnable = SystemUtil.getBoolean("SYNC_USER_ENABLE",false);
//            if (StringUtils.isBlank(suposAddress) || !syncUserEnable) {
//                log.warn("supOS工厂版单点登录跳过：未配置或未开启环境变量 SYNC_USER_ENABLE  SUPOS_EE_ADDRESS");
//                filterChain.doFilter(request, response);
//                return;
//            }
//
//            String code = request.getParameter("code");
//            String state = request.getParameter("state");
//
//            String suposTokenApi = "/open-api/auth/v2/oauth2/token";
//            JSONObject requestBody = new JSONObject();
//            requestBody.put("grantType", "authorization_code");
//            requestBody.put("code", code);
//            requestBody.put("logoutUri", "http://127.0.0.1:8080");
//            Map<String, String> headers = SuposApi.getSignatureHeader(requestBody.toString(), Method.POST, suposTokenApi);
//
//            HttpResponse res = HttpRequest.post(suposAddress + suposTokenApi)
//                    .body(requestBody.toJSONString())
//                    .headerMap(headers, true).execute();
//            log.info("cascade SUPOS 单点登录 请求：requestBody :{}", requestBody);
//            if (res.getStatus() != 200) {
//                log.error("SUPOS 单点登录失败，调用supOS Token接口返回异常 code:{} body:{}", res.getStatus(), res.body());
//                filterChain.doFilter(request, response);
//                return;
//            }
//            JSONObject tokenInfo = JSONObject.parseObject(res.body());
//            String username = tokenInfo.getString("username");
//            log.info("cascade SUPOS 单点登录 返回结果：tokenInfo :{}", tokenInfo);
//            UserManageVo userManageVo = userMapper.getByUsername(username.toLowerCase());
//            //用户不存在时，创建用户
//            if (userManageVo == null) {
//                String createUserJson = "{\"username\":\"" + username + "\",\"password\":\"Supos1304@\",\"roleList\":[{\"roleId\":\"71dd6dc2-6b12-4273-9ec0-b44b86e5b500\",\"roleName\":\"一般用户\"}],\"enabled\":true}";
//                AddUserDto addUserDto = JSONObject.parseObject(createUserJson, AddUserDto.class);
//                addUserDto.setSource("external");
//                ResultVO userRes = userManageService.createUser(addUserDto);
//                log.info("cascade SUPOS 单点登录 用户创建完成 创建结果：{}", JSON.toJSONString(userRes));
//            }
//            TokenVo tokenVo = passwordFreeLogin(username);
//            //替换掉前缀
//            String redirectUri = path.replaceAll(URL_PREFIX, "");
//            String newUri = systemConfig.getEntranceUrl() + "/freeLogin?token=" + tokenVo.getToken() + "&redirectUri=" + redirectUri;
//            // 发送重定向响应，客户端会跳转到该地址
//            httpResponse.sendRedirect(newUri);
//            log.info("cascade SUPOS 单点登录 免登完成，重定向地址:{}", newUri);
//            return;
//        }
//        // 放行
//        filterChain.doFilter(request, response);
//    }
//
//    public TokenVo passwordFreeLogin(String username) {
//        UserManageVo userManageVo = userMapper.getByUsername(username.toLowerCase());
//        if (userManageVo == null) {
//            throw new BuzException("免密登录失败，用户名不存在");
//        }
//
//        String result = keycloakUtil.getUserExchangeTokenById(userManageVo.getId());
//        if (StringUtils.isBlank(result)) {
//            log.warn("免密登录失败，getUserExchangeTokenById 返回为空");
//            throw new BuzException("免密登录失败，getUserExchangeTokenById 返回为空");
//        }
//
//        com.alibaba.fastjson.JSONObject tokenObj = com.alibaba.fastjson.JSONObject.parseObject(result);
//
//        //设置 token与token_info
//        String token = IdUtil.fastUUID();
//        tokenCache.put(token, tokenObj, Constants.TOKEN_MAX_AGE * 1000);
//
//        UserInfoVo userInfoVo = authService.getUserInfoVoByToken(token);
//        if (userInfoVo == null) {
//            log.warn("用户免密登录失败 userInfoVo == null");
//            throw new BuzException("用户免密登录失败");
//        }
//        JWT jwt = JWT.of(tokenObj.getString("access_token"));
//        String sub = jwt.getPayloads().getStr("sub");
//        userInfoVo.setFirstTimeLogin(0);
//        userInfoVo.setTipsEnable(0);
//        userInfoCache.put(sub, userInfoVo);
//        log.info("用户免密登录成功：token:{},userinfo:{}", token, userInfoVo);
//        return new TokenVo(token, Constants.TOKEN_MAX_AGE);
//    }
//
//    @Override
//    public void destroy() {
//        Filter.super.destroy();
//    }
//}