package com.supos.uns;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supos.common.Constants;
import com.supos.uns.service.WebsocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;

@Component
@Slf4j
public class UnsWebsocketHandler implements WebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebsocketService websocketService;


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


}
