package com.supos.uns.openapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelOpenVo {

    @Schema(description = "标签ID")
    String id;

    @Schema(description = "标签名称，新建标签时，必传")
    String labelName;

    @Schema(description = "创建时间--单位：毫秒")
    Long createTime;
}
