package com.supos.i18n.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 语言添加参数
 * @date 2025/9/2 13:29
 */
@Data
public class AddLanguageDto implements Serializable {

    private String flagNo;

    @Schema(description = "语言编码")
    private String languageCode;

    @Schema(description = "语言名称")
    private String languageName;

    /**
     *
     */
    private Integer languageType;

    private Boolean hasUsed;
}
