package com.supos.i18n.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ResourceDto
 * @date 2025/9/4 9:03
 */
@Data
public class ResourceDto implements Serializable {

    private String moduleCode;

    /**
     * 国际化key
     */
    private String i18nKey;

    /**
     * 国际化值
     */
    private String i18nValue;

    private String flagNo;
}
