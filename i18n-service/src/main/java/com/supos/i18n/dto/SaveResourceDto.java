package com.supos.i18n.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化保存参数
 * @date 2025/9/1 16:29
 */
@Data
public class SaveResourceDto implements Serializable {

    /**
     * 国际化健
     */
    private String key;
    /**
     * 模块编码
     */
    private String moduleCode;
    /**
     * 国际化值
     */
    private Map<String, String> values;
}
