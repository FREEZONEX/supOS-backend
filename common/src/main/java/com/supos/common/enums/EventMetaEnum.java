package com.supos.common.enums;

public enum EventMetaEnum implements IEventMetaEnum {

  USER_CHANGE("user"),
  ROLE_CHANGE("role"),
  UNS_FILED_CHANGE("field"),
  ;

  EventMetaEnum(String code) {
    this.code = code;

  }


  private String code;

  @Override
  public String getCode() {
    return this.code;
  }
}
