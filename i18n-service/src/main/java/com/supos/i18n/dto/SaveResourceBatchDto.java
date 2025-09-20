package com.supos.i18n.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化保存参数
 * @date 2025/9/2 18:46
 */
@Data
public class SaveResourceBatchDto implements Serializable {

    private String moduleCode;

    private String languageCode;

    private List<ResourceDto> resources;

}
