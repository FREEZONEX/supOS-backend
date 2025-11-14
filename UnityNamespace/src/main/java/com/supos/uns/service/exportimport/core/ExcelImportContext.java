package com.supos.uns.service.exportimport.core;

import com.supos.common.Constants;
import com.supos.common.dto.excel.ExcelUnsWrapDto;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.RunningStatus;
import com.supos.uns.vo.CreateTemplateVo;
import lombok.Getter;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelImportContext
 * @date 2025/4/22 19:23
 */
@Getter
public class ExcelImportContext {
    private String language;

    private Consumer<RunningStatus> consumer;

    public static final int REFER_DATATYPE = -1;

    private String file;
    private String fileType;

    private Map<String, String> excelCheckErrorMap = new HashMap<>(4);
    private Map<Integer, Map<Integer, String>> error = new HashMap<>();

    /**待导入的模板*/
    private Map<String, CreateTemplateVo> templateMap = new HashMap<>();
    /**待导入的标签*/
    private Set<String> labels = new HashSet<>();
    /**待导入的文件夹*/
    private Map<String, ExcelUnsWrapDto> folderMap = new HashMap<>();
    /**待导入文件夹（导入文件夹或文件时上级文件夹不存在，需要尝试自动创建）*/
    private Map<ExcelUnsWrapDto, String> autoFolderMap = new HashMap<>();

    /**待导入的时序文件*/
    private Map<String, ExcelUnsWrapDto> fileTimeseriesMap = new HashMap<>();
    /**待导入的关系文件*/
    private Map<String, ExcelUnsWrapDto> fileRelationMap = new HashMap<>();
    /**待导入的计算文件*/
    private Map<String, ExcelUnsWrapDto> fileCalculateMap = new HashMap<>();
    /**待导入的聚合文件*/
    private Map<String, ExcelUnsWrapDto> fileAggregationMap = new HashMap<>();
    /**待导入的引用文件*/
    private Map<String, ExcelUnsWrapDto> fileReferenceMap = new HashMap<>();
    /**待导入的JSONB文件*/
    private Map<String, ExcelUnsWrapDto> fileJsonbMap = new HashMap<>();

    /**待导入的引用源文件*/
    private Map<String, ExcelUnsWrapDto> fileSourceMap = new HashMap<>();

    /**导入文件包含的别名*/
    private Set<String> aliasInImportFile = new HashSet<>();
    /**导入文件包含的path*/
    private Set<String> pathInImportFile = new HashSet<>();
    private Map<String, ExcelUnsWrapDto> folderAliasMapInImportFile = new HashMap<>();



    private Set<String> checkTemplateAlias = new HashSet<>();
    private Set<String> checkLabels = new HashSet<>();
    private Set<String> checkReferPaths = new HashSet<>();
    private Set<String> checkReferAliass = new HashSet<>();

    /**导入时用于临时存放从DB查询到的alias*/
    private Set<String> tempAliasFromDb  = new HashSet<>();

    private ExcelTypeEnum activeExcelType = ExcelTypeEnum.Explanation;

    public ExcelImportContext(String file, String fileType, Consumer<RunningStatus> consumer, String language) {
        this.file = file;
        this.fileType = fileType;
        this.consumer = consumer;
        this.language = language;
    }

    public boolean dataEmpty() {
        //TODO 待完善
        return false;
    }

    public void addError(String key, String error) {
        excelCheckErrorMap.put(key, error);
    }

    public void addAllError(Map<String, String> errorMap) {
        excelCheckErrorMap.putAll(errorMap);
    }

    /**
     * 添加需要保存的模板
     * @param templateVo
     */
    public void addTemplateVo(CreateTemplateVo templateVo) {
        templateMap.put(templateVo.getAlias(), templateVo);
    }

    /**
     * 需要保存的模板数量
     * @return
     */
    public int templateSize() {
        return templateMap.size();
    }

    public boolean containTemplateAliasInImportFile(String alias) {
        return templateMap.containsKey(alias);
    }

    /**
     * 添加需要保存的标签
     * @param label
     */
    public void addLabel(String label) {
        labels.add(label);
    }

    /**
     * 需要保存的标签数量
     * @return
     */
    public int labelSize() {
        return labels.size();
    }

    /**
     * 需要保持的文件夹、文件
     * @param uns
     */
    public void addUns(ExcelUnsWrapDto uns) {
        Integer pathType = uns.getPathType();
        if (pathType == Constants.PATH_TYPE_DIR) {
            folderMap.put(uns.getAlias(), uns);
            folderAliasMapInImportFile.put(uns.getAlias(), uns);
        } else {
            Integer dataType = uns.getDataType();
            if (dataType == Constants.TIME_SEQUENCE_TYPE) {
                fileTimeseriesMap.put(uns.getAlias(), uns);
            } else if (dataType == Constants.RELATION_TYPE) {
                fileRelationMap.put(uns.getAlias(), uns);
            } else if (dataType == Constants.CALCULATION_REAL_TYPE) {
                fileCalculateMap.put(uns.getAlias(), uns);
            } else if (dataType == Constants.MERGE_TYPE) {
                fileAggregationMap.put(uns.getAlias(), uns);
            } else if (dataType == Constants.CITING_TYPE) {
                fileReferenceMap.put(uns.getAlias(), uns);
            } else if (dataType == Constants.JSONB_TYPE) {
                fileJsonbMap.put(uns.getAlias(), uns);
            }
        }
        aliasInImportFile.add(uns.getAlias());
        pathInImportFile.add(uns.getAlias());

    }

    public void addAutoFolder(ExcelUnsWrapDto uns, String autoFolder) {
        autoFolderMap.put(uns, autoFolder);
    }

    public void addUnsToReferSource(ExcelUnsWrapDto uns) {
        fileSourceMap.put(uns.getPath(), uns);
    }

    public boolean containAliasInImportFile(String alias) {
        return aliasInImportFile.contains(alias);
    }

    public boolean containPathInImportFile(String path) {
        return pathInImportFile.contains(path);
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

    public boolean pathIsReferSource(String path) {
        return checkReferPaths.contains(path);
    }

    public void addCheckReferAlias(String alias) {
        checkReferAliass.add(alias);
    }

    public boolean aliasIsReferSource(String alias) {
        return checkReferAliass.contains(alias);
    }

    public void setActiveExcelType(ExcelTypeEnum currentExcelType) {
        if (activeExcelType == ExcelTypeEnum.Explanation) {
            activeExcelType = currentExcelType;
        }
    }

    public void clear(ExcelTypeEnum excelTypeEnum, int dataType) {

        //checkTemplateAlias = new HashSet<>();
        //checkLabels = new HashSet<>();
        //checkReferPaths = new HashSet<>();

        if (excelTypeEnum == ExcelTypeEnum.File) {
            if (dataType == REFER_DATATYPE) {
                fileSourceMap = new HashMap<>();
            } else if (dataType == Constants.TIME_SEQUENCE_TYPE) {
                fileTimeseriesMap = new HashMap<>();
            } else if (dataType == Constants.RELATION_TYPE) {
                fileRelationMap = new HashMap<>();
            }else if (dataType == Constants.CALCULATION_REAL_TYPE) {
                fileCalculateMap = new HashMap<>();
            }else if (dataType == Constants.MERGE_TYPE) {
                fileAggregationMap = new HashMap<>();
            }else if (dataType == Constants.CITING_TYPE) {
                fileReferenceMap = new HashMap<>();
            }else if (dataType == Constants.JSONB_TYPE) {
                fileJsonbMap = new HashMap<>();
            }
        }
    }

    public void clearAfterTemplate() {
        templateMap.clear();
    }

    public void clearAfterLabel() {
        labels.clear();
    }

    public void clearAfterFolder() {
        autoFolderMap.clear();
    }
}
