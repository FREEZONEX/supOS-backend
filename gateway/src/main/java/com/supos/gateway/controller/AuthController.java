package com.supos.gateway.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.supos.common.Constants;
import com.supos.common.config.OAuthKeyCloakConfig;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.KeycloakUtil;
import com.supos.common.utils.ServletUtil;
import com.supos.common.vo.UserInfoVo;
import com.supos.gateway.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/inter-api/supos/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private OAuthKeyCloakConfig keyCloakConfig;
    @Autowired
    private KeycloakUtil keycloakUtil;

    /**
     * OAuth 获取token
     */
    @GetMapping("/token")
    public void getToken(HttpServletRequest request, HttpServletResponse response) {
        String code = request.getParameter("code");
        log.info(">>>>>>>>>>>>>>>>>getToken code:{}", code);
        if (StrUtil.isBlank(code)) {
            log.info(">>>>>>>>>>>>>getToken失败，授权码code为空");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        //通过code获取token 并设置缓存
        String token = authService.getTokenByCode(code);
        if (StrUtil.isBlank(token)) {
            log.info(">>>>>>>>>>>>>getToken失败，token获取为空");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        Cookie cookie = new Cookie(Constants.ACCESS_TOKEN_KEY, token);
        cookie.setPath("/");      // 设置路径
        cookie.setMaxAge(Constants.COOKIE_MAX_AGE);
        response.addCookie(cookie);
        response.setStatus(HttpStatus.FOUND.value());
        UserInfoVo userInfoVo = authService.getUserInfoVoByToken(token);
        if (userInfoVo != null && StringUtils.hasText(userInfoVo.getHomePage())) {
            response.addHeader("Location", userInfoVo.getHomePage());
        } else {
            response.addHeader("Location", keyCloakConfig.getSuposHome());
        }
        log.info(">>>>>>>>>>>>>getToken成功 token:{}", token);
    }

    /**
     * 根据Token返回用户信息
     * 如不存在Http Code依然为200，用户信息为空
     */
    @GetMapping("/user")
    public ResponseEntity<ResultVO<UserInfoVo>> getUserInfoVoByToken(HttpServletRequest request) {
        String authEnable = SystemUtil.get("SYS_OS_AUTH_ENABLE", "false");
        Cookie cookie = ServletUtil.getCookie(request, Constants.ACCESS_TOKEN_KEY);
        if (cookie == null) {
            if ("false".equals(authEnable)){
                return ResponseEntity.ok(ResultVO.successWithData(UserInfoVo.guest()));
            }
            return ResponseEntity.ok(ResultVO.success("not found user info"));
        }
        UserInfoVo vo = authService.getUserInfoVoByToken(cookie.getValue());
        if (null == vo) {
            if ("false".equals(authEnable)){
                return ResponseEntity.ok(ResultVO.successWithData(UserInfoVo.guest()));
            }
            keycloakUtil.removeSession(cookie.getValue());
            return ResponseEntity.ok(ResultVO.success("not found user info"));
        }
        ResponseCookie newCookie = ResponseCookie.from(Constants.ACCESS_TOKEN_KEY, cookie.getValue())
                .path("/")
                .maxAge(Constants.COOKIE_MAX_AGE) // 秒
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, newCookie.toString());
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(ResultVO.successWithData(vo));
    }

    /**
     * 用于网关鉴权Token
     * （保活）
     */
    @GetMapping("/userinfo")
    public ResponseEntity getUserInfoByToken(HttpServletRequest request) {
        Cookie cookie = ServletUtil.getCookie(request, Constants.ACCESS_TOKEN_KEY);
        if (null == cookie) {
            log.error(">>>>>>>>>>>>>>>Cookie获取失败 cookie:null");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return authService.getUserInfoByToken(cookie.getValue());
    }

    @GetMapping("/code")
    public ResponseEntity<String> code(HttpServletRequest request) {
        String code = request.getParameter("code");
        return ResponseEntity.ok().body(code);
    }

    @DeleteMapping("/logout")
    public ResultVO logout(HttpServletRequest request) {
        Cookie cookie = ServletUtil.getCookie(request, Constants.ACCESS_TOKEN_KEY);
        if (null != cookie) {
            return authService.logout(cookie.getValue());
        }
        return ResultVO.success("ok");
    }

    @GetMapping("/test")
    public ResponseEntity test(@RequestParam String id) {
        UserInfoVo userInfoVo = new UserInfoVo();
        userInfoVo.setSub(id);
        userInfoVo = authService.getUserRolesResources(userInfoVo);
        return ResponseEntity.ok(userInfoVo);
    }

}
