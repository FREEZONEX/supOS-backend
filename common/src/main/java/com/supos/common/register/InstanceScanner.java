package com.supos.common.register;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

public class InstanceScanner extends ClassPathBeanDefinitionScanner {

    private Class<? extends Annotation> type;

    public InstanceScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> type) {
        super(registry, false);
        this.type = type;
    }

    public void registerTypeFilter() {
        addIncludeFilter(new AnnotationTypeFilter(type));
    }
}
