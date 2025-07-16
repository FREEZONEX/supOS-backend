package com.supos.ws.register;

import com.supos.common.enums.WSActionEnum;
import com.supos.ws.action.ActionApi;
import com.supos.ws.dto.ActionBaseRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Map;

@Slf4j
public class WebsocketActionContext {

    private Map<Object, Class<?>> instanceMap;

    private ApplicationContext applicationContext;

    public WebsocketActionContext(Map<Object, Class<?>> instanceMap, ApplicationContext applicationContext) {
        this.instanceMap = instanceMap;
        this.applicationContext = applicationContext;
    }

    public ActionApi<ActionBaseRequest> getInstance(WSActionEnum wsAction) {
        if (wsAction == null) {
            return null;
        }
        Class<?> aClass = instanceMap.get(wsAction);
        if (aClass == null) {
            log.error("当前action的枚举类不存在，协议类型： {}", wsAction.name());
            return null;
        }
        return (ActionApi<ActionBaseRequest>)applicationContext.getBean(aClass);
    }

}
