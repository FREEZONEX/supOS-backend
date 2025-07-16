package com.supos.ws.register;

import com.supos.common.annotation.WSAction;
import com.supos.common.register.AbstractBeanRegister;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebsocketActionServiceRegister extends AbstractBeanRegister implements ApplicationContextAware {

    private static final String INSTANCE_PACKAGE_PATH = "com.supos.ws.action";

    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Map<Object, Class<?>> instanceMap = super.scanInstancePath(INSTANCE_PACKAGE_PATH, WSAction.class);
        WebsocketActionContext wsContext = new WebsocketActionContext(instanceMap, applicationContext);
        beanFactory.registerSingleton(WebsocketActionContext.class.getName(), wsContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
