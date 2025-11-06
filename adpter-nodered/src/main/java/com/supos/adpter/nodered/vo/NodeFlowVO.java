package com.supos.adpter.nodered.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeFlowVO implements Serializable {

    private static final long serialVersionUID = 1l;

    private String id;

    private String flowName;

    private String flowId;

    private String description;

    private String flowStatus;

    private String template;

    private Integer mark;

    private String creator;

    private long createTime;

}
