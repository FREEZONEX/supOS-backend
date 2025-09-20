package com.supos.i18n.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: I18nResourceVO
 * @date 2025/9/1 14:29
 */
@Data
public class I18nResourceVO implements Serializable {

    /**
     * 国际化key
     */
    @Schema(description = "国际化key")
    private String i18nKey;

    @Schema(description = "国际化值")
    private Map<String, String> values;
}
