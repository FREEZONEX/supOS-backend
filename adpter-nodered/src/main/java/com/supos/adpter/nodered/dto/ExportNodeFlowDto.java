package com.supos.adpter.nodered.dto;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportNodeFlowDto {

    private List<NodeFlowDto> flows;

    private List<NodeFlowModelDto> flowRefs;

    private JSONArray tags;

    private JSONArray nodes;

    private JSONArray globalNodes;


}
