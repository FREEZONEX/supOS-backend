package com.supos.i18n.service.excel.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.supos.i18n.common.ModuleType;
import com.supos.i18n.dto.AddModuleDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleData implements Serializable {
    @ExcelProperty(index = 0)
    private String moduleCode;
    @ExcelProperty(index = 1)
    private String moduleName;

    @ExcelIgnore
    private boolean checkSuccess;
    @ExcelIgnore
    private String flagNo;

    public AddModuleDto toAddModuleDto() {
        AddModuleDto addModuleDto = new AddModuleDto();
        addModuleDto.setModuleCode(moduleCode);
        addModuleDto.setModuleName(moduleName);
        addModuleDto.setModuleType(ModuleType.CUSTOM.getType());
        addModuleDto.setFlagNo(flagNo);
        return addModuleDto;
    }
}