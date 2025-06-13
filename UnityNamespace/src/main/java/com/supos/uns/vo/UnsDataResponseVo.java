package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnsDataResponseVo {

    @Schema(description = "不存在的文件别名列表")
    List<String> notExists;

    @Schema(description = "写入错误的字段")
    Map<String,String> errorFields;
}
