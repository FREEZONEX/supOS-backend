package com.supos.common.event.mount;

import com.supos.common.enums.mount.MountEventType;
import com.supos.common.enums.mount.MountSourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 挂载源在离线事件
 * @date 2025/6/17 9:58
 */
@Getter
public class MountSourceOnlineEvent extends ApplicationEvent {

    private MountSourceType sourceType;

    private String sourceAlias;
    private Consumer<Boolean> callback;

    public MountSourceOnlineEvent(Object source, MountSourceType sourceType, String sourceAlias, Consumer<Boolean> callback) {
        super(source);
        this.sourceType = sourceType;
        this.sourceAlias = sourceAlias;
        this.callback = callback;
    }
}
