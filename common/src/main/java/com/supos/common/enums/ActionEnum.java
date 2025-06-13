package com.supos.common.enums;

public enum ActionEnum implements IActionEnum {

  ADD("add"),
  MODIFY("modify"),
  DELETE("delete"),
  ;

  ActionEnum(String code) {
    this.code = code;
  }


  private String code;

  @Override
  public String getCode() {
    return this.code;
  }
}
