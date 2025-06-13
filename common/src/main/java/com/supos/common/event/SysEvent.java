package com.supos.common.event;

import com.supos.common.enums.IActionEnum;
import com.supos.common.enums.IEventMetaEnum;
import com.supos.common.enums.IServiceEnum;
import org.springframework.context.ApplicationEvent;

public class SysEvent extends ApplicationEvent {

  public final String service;
  public final String eventMeta;
  public final String action;
  public final Object payload;

  public SysEvent(Object source, IServiceEnum service, IEventMetaEnum eventMeta,
      IActionEnum actionEnum, Object payload) {
    super(source);
    this.service = service.getCode();
    this.eventMeta = eventMeta.getCode();
    this.action = actionEnum.getCode();
    this.payload = payload;
  }

  public SysEvent(Object source, String serviceCode, String eventMetaCode,
      String actionCode, Object payload) {
    super(source);
    this.service = serviceCode;
    this.eventMeta = eventMetaCode;
    this.action = actionCode;
    this.payload = payload;
  }
}
