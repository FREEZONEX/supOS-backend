package com.supos.i18n.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 模块添加参数
 * @date 2025/9/2 10:30
 */
@Data
public class AddModuleDto implements Serializable {

    private String flagNo;

    private String moduleCode;

    private String moduleName;

    private Integer moduleType;
}
