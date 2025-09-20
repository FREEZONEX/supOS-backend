package com.supos.i18n.service.excel.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.supos.i18n.common.ModuleType;
import com.supos.i18n.dto.AddLanguageDto;
import com.supos.i18n.dto.AddModuleDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LanguageData implements Serializable {
    @ExcelProperty(index = 0)
    private String code;
    @ExcelProperty(index = 1)
    private String name;

    @ExcelIgnore
    private boolean checkSuccess;
    @ExcelIgnore
    private String flagNo;

    public AddLanguageDto toAddLanguageDto() {
        AddLanguageDto languageDto = new AddLanguageDto();
        languageDto.setLanguageCode(code);
        languageDto.setLanguageName(name);
        languageDto.setFlagNo(flagNo);
        return languageDto;
    }
}