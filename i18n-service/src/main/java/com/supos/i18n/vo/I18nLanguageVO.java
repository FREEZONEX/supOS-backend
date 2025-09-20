package com.supos.i18n.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author sunlifang
 * @version 1.0
 * @description: I18nLanguageVO
 * @date 2025/9/1 13:17
 */
@Data
public class I18nLanguageVO implements Serializable {

    /**
     * 主键
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 语言code码 eg:zn_CH
     */
    @Schema(description = "语言code码 eg:zn_CH")
    private String languageCode;

    /**
     * 语言类型(code自己对应的语言描述)
     */
    @Schema(description = "语言类型(code自己对应的语言描述)")
    private String languageName;

    /**
     *
     */
    @Schema(description = "模块类型，1-内置；2-自定义")
    private Integer languageType;

    /**
     * 是否启用 0 不使用 1使用
     */
    @Schema(description = "是否启用 0 不使用 1使用")
    private Boolean  hasUsed;
}
