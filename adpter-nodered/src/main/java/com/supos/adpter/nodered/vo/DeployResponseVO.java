package com.supos.adpter.nodered.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeployResponseVO {

    private String flowId;

    private String version;
}
