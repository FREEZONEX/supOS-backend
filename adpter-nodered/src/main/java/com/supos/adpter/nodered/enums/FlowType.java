package com.supos.adpter.nodered.enums;

import lombok.Getter;

@Getter
public enum FlowType {
    NODERED("node-red"),

    EVENTFLOW("event-flow");


    private String flowName;

    FlowType(String flowName) {
        this.flowName = flowName;
    }
}
