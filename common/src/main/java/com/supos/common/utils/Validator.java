package com.supos.common.utils;


import cn.hutool.core.util.StrUtil;
import com.supos.common.enums.IBaseEnum;
import com.supos.common.exception.BuzException;

public class Validator {

  public static void assertTrue(boolean param, IBaseEnum errCode, String... parameters) {
    if (!param) {
      throw new BuzException(errCode.getCode(), parameters);
    }
  }

  public static void assertFalse(boolean param, IBaseEnum errCode, String... parameters) {
    if (param) {
      throw new BuzException(errCode.getCode(), parameters);
    }
  }

  public static void assertNull(Object param, IBaseEnum errCode, String... parameters) {
    if (param != null) {
      throw new BuzException(errCode.getCode(), parameters);
    }
  }

  public static void assertNotNull(Object param, IBaseEnum errCode, String... parameters) {
    if (param == null) {
      throw new BuzException(errCode.getCode(), parameters);
    }
  }

  public static void assertNotBlank(String param, IBaseEnum errCode, String... parameters) {
    if (StrUtil.isBlank(param)) {
      throw new BuzException(errCode.getCode(), parameters);
    }
  }

}
