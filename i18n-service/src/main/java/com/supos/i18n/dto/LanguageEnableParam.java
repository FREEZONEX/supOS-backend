package com.supos.i18n.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 语言启用参数
 * @date 2025/9/1 14:03
 */
@Data
public class LanguageEnableParam implements Serializable {

    private String languageCode;

    private Boolean enable;
}
