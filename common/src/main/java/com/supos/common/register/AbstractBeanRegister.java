package com.supos.common.register;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBeanRegister implements BeanFactoryPostProcessor {

    protected Map<Object, Class<?>> scanInstancePath(String path, Class<? extends Annotation> clazz){
        Map<Object,Class<?>> instanceMap = new HashMap<>();
        GenericApplicationContext context = new GenericApplicationContext();
        InstanceScanner scanner = new InstanceScanner(context, clazz);
        scanner.registerTypeFilter();
        scanner.scan(path);
        context.refresh();
        context.getBeansWithAnnotation(clazz).forEach((name, bean)-> {
            Annotation annotation = AnnotationUtils.findAnnotation(bean.getClass(), clazz);
            Object type = AnnotationUtils.getValue(annotation, "value");
            if (type instanceof Object[]){
                for (Object obj : (Object[]) type) {
                    instanceMap.put(obj, bean.getClass());
                }
            } else {
                instanceMap.put(type, bean.getClass());
            }
        });
        return instanceMap;
    }
}
