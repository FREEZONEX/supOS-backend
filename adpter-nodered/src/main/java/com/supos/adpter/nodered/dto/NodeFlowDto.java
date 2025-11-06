package com.supos.adpter.nodered.dto;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

@Data
public class NodeFlowDto {

    private long id;

    private String flowId;

    private String flowName;

    private String description;

    private String template;


}
