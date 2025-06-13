package com.supos.uns.service.exportimport.core;

import com.supos.common.dto.excel.ExcelUnsWrapDto;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.uns.vo.CreateTemplateVo;
import lombok.Getter;

import java.util.*;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelImportContext
 * @date 2025/4/22 19:23
 */
@Getter
public class ExcelImportContext {

    private String file;

    private Map<String, String> excelCheckErrorMap = new HashMap<>(4);
    private Map<Integer, Map<Integer, String>> error = new HashMap<>();

    private List<CreateTemplateVo> templateVoList = new LinkedList<>();

    private Set<String> labels = new HashSet<>();

    private Set<String> aliasInExcel = new HashSet<>();
    private Set<String> pathInExcel = new HashSet<>();

    //uns
    private Map<String, ExcelUnsWrapDto> unsMap = new HashMap<>();
    private List<ExcelUnsWrapDto> unsList = new LinkedList<>();


    private Set<String> checkTemplateAlias = new HashSet<>();
    private Set<String> checkLabels = new HashSet<>();
    private Set<String> checkReferPaths = new HashSet<>();
    private Set<String> checkReferAliass = new HashSet<>();

    /**导入时用于临时存放从DB查询到的alias*/
    private Set<String> tempAliasFromDb  = new HashSet<>();

    private ExcelTypeEnum  activeExcelType = ExcelTypeEnum.Explanation;

    public ExcelImportContext(String file) {
        this.file = file;
    }

    public boolean dataEmpty() {
        //TODO 待完善
        return false;
/*        return CollectionUtils.isNotEmpty(templateVoList) && CollectionUtils.isNotEmpty(topicList)
                && CollectionUtils.isNotEmpty(labels) && MapUtils.isEmpty(excelCheckErrorMap);*/
    }

    public void addError(String key, String error) {
        excelCheckErrorMap.put(key, error);
    }

    public void addAllError(Map<String, String> errorMap) {
        excelCheckErrorMap.putAll(errorMap);
    }

    public void addTemplateVo(CreateTemplateVo templateVo) {
        templateVoList.add(templateVo);
    }

    public boolean addAlias(String alias) {
        return aliasInExcel.add(alias);
    }
    public boolean addPath(String path) {
        return pathInExcel.add(path);
    }

    public void addLabel(String label) {
        labels.add(label);
    }

    public void addCheckTemplateAlias(String templateAlias) {
        checkTemplateAlias.add(templateAlias);
    }

    public void addCheckLabel(String label) {
        checkLabels.add(label);
    }

    public void addCheckReferPath(String path) {
        checkReferPaths.add(path);
    }

    public void addCheckReferAlias(String alias) {
        checkReferAliass.add(alias);
    }

    public void setActiveExcelType(ExcelTypeEnum currentExcelType) {
        if (activeExcelType == ExcelTypeEnum.Explanation) {
            activeExcelType = currentExcelType;
        }
    }

    public void clear() {
        templateVoList.clear();
        labels.clear();
        unsMap.clear();
        unsList.clear();

        checkTemplateAlias.clear();
        checkLabels.clear();
        checkReferPaths.clear();
    }
}
