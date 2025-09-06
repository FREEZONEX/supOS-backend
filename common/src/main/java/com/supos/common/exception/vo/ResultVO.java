package com.supos.common.exception.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultVO<T> implements Serializable {

  private static final long serialVersionUID = 1l;

  @Schema(description = "状态码")
  private int code;

  @Schema(description = "提示消息")
  private String msg;

  @Schema(description = "返回数据")
  private T data;

  public static <T> ResultVO successWithData(T data) {
    return ResultVO.builder().code(200).msg("ok").data(data).build();
  }

  public static <T> ResultVO successWithData(String msg, T data) {
    return ResultVO.builder()
        .code(200)
        .msg(msg)
        .data(data)
        .build();
  }

  public static <T> ResultVO success() {
    return ResultVO.builder().code(200).msg("ok").build();
  }

  public static <T> ResultVO success(String msg) {
    return ResultVO.builder().code(200).msg(msg).build();
  }

  public static <T> ResultVO fail(String msg) {
    return ResultVO.builder().code(400).msg(msg).build();
  }


  public ResultVO(boolean expression) {
    this.code = expression ? 200 : 500;
    this.msg = expression ? "success" : "error";
  }

}
