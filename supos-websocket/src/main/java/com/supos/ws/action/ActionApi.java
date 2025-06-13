package com.supos.ws.action;

import com.alibaba.fastjson.JSONObject;
import com.supos.common.enums.WSActionEnum;
import com.supos.ws.dto.ActionBaseRequest;
import com.supos.ws.dto.ActionHeader;
import com.supos.ws.dto.ActionResponse;

public interface ActionApi<T extends ActionBaseRequest> {

    default ActionResponse wrapResponse(String version, WSActionEnum reqAction, String msg, int code) {
        ActionHeader head = ActionHeader.builder().cmd(WSActionEnum.CMD_RESPONSE.getCmdNo()).version(version).build();
        JSONObject data = new JSONObject();
        data.put("requestCmd", reqAction.getCmdNo());
        data.put("msg", msg);
        data.put("code", code);
        return ActionResponse.builder().head(head).data(data).build();
    }

    ActionResponse doAction(String sessionId, T request);

    T parseRequest(String requestBody);
}
