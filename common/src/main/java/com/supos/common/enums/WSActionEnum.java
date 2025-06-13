package com.supos.common.enums;

import lombok.Getter;

@Getter
public enum WSActionEnum {

  CMD_SUBS_EVENT(5),

  CMD_UNSUBS_EVENT(4),

  CMD_PUBLISH_EVENT(6),

  CMD_RESPONSE(2);

  private int cmdNo;

  WSActionEnum(int cmdNo) {
      this.cmdNo = cmdNo;
  }

  public static WSActionEnum getByNo(int cmdNo) {
    for (WSActionEnum we : WSActionEnum.values()) {
        if (we.cmdNo == cmdNo) {
          return we;
        }
    }
    return null;
  }

}


