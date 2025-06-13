package com.supos.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 流程安装事件
 * @date 2025/6/6 13:15
 */
@Getter
public class FlowInstallEvent extends ApplicationEvent {

    public final static String INSTALL = "INSTALL";
    public final static String UNINSTALL = "UNINSTALL";

    private String flowName;
    private String operation;

    public FlowInstallEvent(Object source, String flowName, String operation) {
        super(source);
        this.flowName = flowName;
        this.operation = operation;
    }
}
