package com.supos.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;

@Data
public class LabelVo {

    @Schema(description = "标签ID：已有标签时必传，新建标签时不需要传")
    private String id;

    @Schema(description = "标签名称，新建标签时，必传")
    private String labelName;

    @Schema(description = "创建时间")
    private Date createAt;
}
