package com.supos.common.annotation;

import com.supos.common.enums.IOTProtocol;
import com.supos.common.enums.WSActionEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WSAction {

    WSActionEnum value();
}
