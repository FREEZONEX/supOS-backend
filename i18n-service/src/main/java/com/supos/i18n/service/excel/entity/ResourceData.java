package com.supos.i18n.service.excel.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.supos.i18n.common.ModuleType;
import com.supos.i18n.dto.AddModuleDto;
import com.supos.i18n.dto.ResourceDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceData implements Serializable {
    @ExcelProperty(index = 0)
    private String moduleCode;
    @ExcelProperty(index = 1)
    private String key;
    @ExcelProperty(index = 2)
    private String value;

    @ExcelIgnore
    private boolean checkSuccess;
    @ExcelIgnore
    private String flagNo;

    public ResourceDto toResourceDto() {
        ResourceDto resourceDto = new ResourceDto();
        resourceDto.setModuleCode(moduleCode);
        resourceDto.setI18nKey(key);
        resourceDto.setI18nValue(value);
        resourceDto.setFlagNo(flagNo);
        return resourceDto;
    }
}