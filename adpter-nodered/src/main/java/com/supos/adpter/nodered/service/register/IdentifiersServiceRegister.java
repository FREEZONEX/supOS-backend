package com.supos.adpter.nodered.service.register;

import com.supos.common.annotation.ProtocolIdentifierProvider;
import com.supos.common.register.AbstractBeanRegister;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IdentifiersServiceRegister extends AbstractBeanRegister implements ApplicationContextAware {

    private static final String INSTANCE_PACKAGE_PATH = "com.supos.adpter.nodered.service.enums";

    private ApplicationContext applicationContext;


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Map<Object, Class<?>> instanceMap = super.scanInstancePath(INSTANCE_PACKAGE_PATH, ProtocolIdentifierProvider.class);
        IdentifiersContext identifiersContext = new IdentifiersContext(instanceMap, applicationContext);
        beanFactory.registerSingleton(IdentifiersContext.class.getName(), identifiersContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
