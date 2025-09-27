package com.supos.uns;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supos.common.Constants;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import com.supos.uns.i18n.I18nResourceManager;
import com.supos.uns.service.WebsocketService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;

@Component
@Slf4j
public class UnsWebsocketHandler implements WebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private I18nResourceManager i18nResourceManager;

    @Resource
    private TimedCache<String, JSONObject> tokenCache;

    @Resource
    private TimedCache<String, UserInfoVo> userInfoCache;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("WebSocket open: {}", session);
        synchronized (UnsWebsocketHandler.class) {
            if (websocketService.getSessions().size() > Constants.WS_SESSION_LIMIT) {
                try {
                    session.sendMessage(new TextMessage("session reached its maximum capacity " + Constants.WS_SESSION_LIMIT));
                    session.close();
                } catch (IOException e) {
                    //
                }
                log.error("ws会话超过系统限制（{}），当前会话关闭", Constants.WS_SESSION_LIMIT);
                return;
            }
            websocketService.addSession(session);
        }
        // 设置用户上下文
        UserInfoVo userInfoVo = parserUserFromToken(session);
        // 设置国际化上下文
        parserI18n(userInfoVo != null ? userInfoVo.getSub() : null);
        websocketService.handleSessionConnected(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            if (!(message instanceof TextMessage)) {
                return;
            }
            TextMessage textMessage = (TextMessage) message;

            String payload = textMessage.getPayload();
            log.trace("WebSocket handleMessage[{}] : {}", session.getId(), payload);

            //heartbeat
            if ("ping".equals(payload)) {
                session.sendMessage(new TextMessage("pong"));
                return;
            }
            websocketService.handleCmdMsg(payload, session);
        } catch (Exception e) {
            log.error(">>>>>>>>>>>>handleMessage IOException", e);
        }
    }

    void uncaughtException(Thread t, Throwable e) {
        log.error("发送Ws数据失败: " + t.getName(), e);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        if (exception != null && exception.getClass() != java.io.EOFException.class) {
            log.error("WebSocket handleTransportError[{}]", session.getId(), exception);
        }
        try {
            session.close();
        } catch (IOException e) {
            //
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            session.close();
        } catch (IOException e) {
            //
        }
        final String connectionId = session.getId();
        log.debug("ws ConnectionClosed: {}, status is {}", connectionId, status.getReason());
        websocketService.handleSessionClosed(connectionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false; // 不支持部分消息
    }


    private UserInfoVo parserUserFromToken(WebSocketSession session) {
        UriComponents components = UriComponentsBuilder.fromUri(session.getUri()).build();
        String token = components.getQueryParams().getFirst("token");

        if (null != token) {
            JSONObject tokenObj = tokenCache.get(token);
            if (null != token && tokenObj != null) {
                String accessToken = tokenObj.getString("access_token");
                JWT jwt = JWT.of(accessToken);
                String sub = jwt.getPayloads().getStr("sub");
                UserInfoVo userInfoVo = userInfoCache.get(sub);
                UserContext.set(userInfoVo);
                log.debug("set user content success!");
            }
        }
        return UserContext.get();
    }

    private void parserI18n(String userId) {
        String mainLanguage = i18nResourceManager.getMainLanguage(userId);
        log.info("mainLanguage: {}", mainLanguage);
        if (mainLanguage != null) {
            LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(new Locale(mainLanguage)),  true);
        }
    }
}
