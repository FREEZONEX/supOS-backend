package com.supos.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.function.Consumer;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 插件卸载前置事件
 * @date 2025/5/29 10:32
 */
@Getter
public class PluginPreUnInstallEvent extends ApplicationEvent {

    private String pluginName;

    private Consumer<String> uninstallCallback;

    public PluginPreUnInstallEvent(Object source, String pluginName, Consumer<String> uninstallCallback) {
        super(source);
        this.pluginName = pluginName;
        this.uninstallCallback = uninstallCallback;
    }
}
