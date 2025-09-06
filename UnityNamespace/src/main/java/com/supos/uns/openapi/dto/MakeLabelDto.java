package com.supos.uns.openapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class MakeLabelDto {


    @Schema(description = "文件别名")
    private String fileAlias;

    @Schema(description = "标签名称数组，如标签名称不存在，系统会自动创建")
    private List<String> labelNames;

}
