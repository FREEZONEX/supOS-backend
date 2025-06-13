package com.supos.ws;

import com.alibaba.fastjson.JSON;
import com.supos.common.enums.WSActionEnum;
import com.supos.ws.action.ActionApi;
import com.supos.ws.dto.ActionBaseRequest;
import com.supos.ws.dto.ActionResponse;
import com.supos.ws.register.WebsocketActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

@Component
@Slf4j
public class WebsocketServer implements WebSocketHandler {

    @Autowired
    private WebsocketSessionManager wssm;
    @Autowired
    private WebsocketActionContext websocketActionContext;

    /**
     * 处理ws client连接成功
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        wssm.put(session.getId(), session);
    }


    /**
     * 接收client发送过来的数据
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        ActionBaseRequest actionRequest = null;
        String payload = "";
        try {
            payload = ((TextMessage) message).getPayload();
            actionRequest = JSON.parseObject(payload, ActionBaseRequest.class);
        } catch (Exception e) {
            log.error("ws: 接收到的消息非json格式,或者json字段类型不匹配， 内容: {}", message.getPayload(), e);
            session.sendMessage(new PongMessage());
            return;
        }
        WSActionEnum actionEnum = WSActionEnum.getByNo(actionRequest.getHead().getCmd());
        if (actionEnum == null) {
            log.error("ws: 当前cmd不支持, cmd={}", actionRequest.getHead().getCmd());
            session.sendMessage(new TextMessage("不支持当前cmd"));
            return;
        }
        ActionApi<ActionBaseRequest> actionService = websocketActionContext.getInstance(actionEnum);
        if (actionService == null) {
            session.sendMessage(new TextMessage("当前cmd暂未实现"));
            return;
        }
        // 用具体的实现类再解析一遍，获取真正的请求对象
        actionRequest = actionService.parseRequest(payload);
        ActionResponse actionResponse = actionService.doAction(session.getId(), actionRequest);
        // 返回反馈
        if (actionResponse != null) {
            session.sendMessage(new TextMessage(JSON.toJSONString(actionResponse)));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("ws: websocket传输异常 sessionId={}", session.getId(), exception);
        /*if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        wssm.removeBySessionId(session.getId());*/
    }

    /**
     * 处理ws连接关闭
     * @param session
     * @param closeStatus
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.warn("ws: session已关闭, 原因: {}", closeStatus.getReason());
        wssm.removeBySessionId(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
