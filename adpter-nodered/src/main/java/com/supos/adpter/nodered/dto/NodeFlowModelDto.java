package com.supos.adpter.nodered.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeFlowModelDto {
    // 关联的流程id
    private long pid;

    private String alias;
}
