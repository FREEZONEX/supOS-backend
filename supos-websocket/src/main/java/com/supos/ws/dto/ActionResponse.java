package com.supos.ws.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActionResponse {

    private ActionHeader head;

    private JSONObject data;

}
