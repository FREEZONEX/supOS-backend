package com.supos.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelVo {

    @Schema(description = "标签ID：已有标签时必传，新建标签时不需要传")
    private String id;

    @Schema(description = "标签名称，新建标签时，必传")
    private String labelName;

    @Schema(description = "创建时间")
    private Date createAt;

    @Schema(description = "主题")
    String topic;

    @Schema(description = "是否订阅")
    Boolean subscribeEnable;

    @Schema(description = "订阅频率")
//    @TimeIntervalValidator(field = "subscribeFrequency")
    String subscribeFrequency;

    @Schema(description = "创建时间--单位：毫秒")
    Long createTime;

    @Schema(description = "订阅时间")
    private Date subscribeAt;
}
