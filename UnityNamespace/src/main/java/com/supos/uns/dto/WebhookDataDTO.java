package com.supos.uns.dto;

import lombok.Data;

@Data
public class WebhookDataDTO {

  private String id;

  private String topic;

  private String alias;

  // 标识是时序类型(1)还是关系类型(2)
  private int type;

  private String config;

  private String fields;
}
