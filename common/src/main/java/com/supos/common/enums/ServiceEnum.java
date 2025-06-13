package com.supos.common.enums;

public enum ServiceEnum implements IServiceEnum {

  AUTH_SERVICE("auth"),
  UNS_SERVICE("uns"),
  ;

  ServiceEnum(String code) {
    this.code = code;

  }


  private String code;

  @Override
  public String getCode() {
    return this.code;
  }
}
