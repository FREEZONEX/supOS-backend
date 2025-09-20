package com.supos.i18n.dto;

import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 导出请求参数
 * @date 2025/9/3 10:22
 */
@Data
public class I18nExportParam {

    private String userId;
    private String languageCode;
}
