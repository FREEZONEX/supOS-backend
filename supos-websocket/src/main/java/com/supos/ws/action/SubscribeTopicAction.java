package com.supos.ws.action;


import com.alibaba.fastjson.JSON;
import com.supos.common.annotation.WSAction;
import com.supos.common.enums.WSActionEnum;
import com.supos.ws.WebsocketSessionManager;
import com.supos.ws.dto.ActionBaseRequest;
import com.supos.ws.dto.ActionResponse;
import com.supos.ws.dto.SubscribeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订阅topic
 */
@WSAction(WSActionEnum.CMD_SUBS_EVENT)
@Service("subscribeTopicAction")
@Slf4j
public class SubscribeTopicAction implements ActionApi<SubscribeRequest> {

    @Autowired
    @Lazy
    private WebsocketSessionManager wssm;

    @Override
    public ActionResponse doAction(String sessionId, SubscribeRequest request) {
        List<String> topics = request.getData().getSource().getTopic();
        if (topics == null || topics.isEmpty()) {
            log.error("ws: 订阅失败，topic列表为空");
            return wrapResponse(request.getHead().getVersion(), WSActionEnum.CMD_SUBS_EVENT, "topic列表为空", HttpStatus.BAD_REQUEST.value());
        }
        for (String topic : topics) {
            boolean result = wssm.binding(topic, sessionId);
            if (!result) {
                log.error("ws: topic和session的关系绑定失败, 订阅不做处理, sessionId={}, topic={}", sessionId, topic);
                return wrapResponse(request.getHead().getVersion(), WSActionEnum.CMD_SUBS_EVENT, "订阅绑定ws异常", HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }
        return wrapResponse(request.getHead().getVersion(), WSActionEnum.CMD_SUBS_EVENT, "success", HttpStatus.OK.value());
    }

    @Override
    public SubscribeRequest parseRequest(String requestBody) {
        return JSON.parseObject(requestBody, SubscribeRequest.class);
    }
}

