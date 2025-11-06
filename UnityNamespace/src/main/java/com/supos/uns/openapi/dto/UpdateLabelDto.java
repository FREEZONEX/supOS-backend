package com.supos.uns.openapi.dto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateLabelDto {


    @Schema(description = "标签ID")
    @Hidden
    private String id;

    @Schema(description = "标签名称")
    @Size(max = 63 , message = "uns.label.length.limit.exceed")
    @NotEmpty(message = "uns.label.name.not.empty")
    private String labelName;

    @Schema(description = "是否订阅")
    Boolean subscribeEnable;

    @Schema(description = "订阅频率")
//    @TimeIntervalValidator(field = "subscribeFrequency")
    String subscribeFrequency;
}
