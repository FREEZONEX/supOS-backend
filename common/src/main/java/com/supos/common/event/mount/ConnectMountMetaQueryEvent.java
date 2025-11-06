package com.supos.common.event.mount;

import com.supos.common.enums.mount.MountMetaQueryType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.function.Consumer;

/**
 * 挂载元数据查询事件
 * @author sunlifang
 * @version 1.0
 * @description: 用于查询挂载的元数据
 * @date 2025/6/17 9:58
 */
@Getter
public class ConnectMountMetaQueryEvent extends ApplicationEvent {

    private Long id;

    private MountMetaQueryType queryType;

    private Object param;

    private Consumer<Object> callback;

    public ConnectMountMetaQueryEvent(Object source, Long id, MountMetaQueryType queryType, Object param, Consumer<Object> callback) {
        super(source);
        this.id = id;
        this.queryType = queryType;
        this.param = param;
        this.callback = callback;
    }
}
