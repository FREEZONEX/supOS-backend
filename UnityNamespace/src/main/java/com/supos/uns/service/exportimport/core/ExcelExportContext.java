package com.supos.uns.service.exportimport.core;

import com.supos.common.Constants;
import com.supos.common.dto.InstanceField;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelExportContext
 * @date 2025/4/23 16:40
 */
@Getter
public class ExcelExportContext {
    private String language;

    /**待导出的模板*/
    private Map<Long, UnsPo> templateMap = new LinkedHashMap<>();
    /**待导出的标签*/
    private Map<Long, UnsLabelPo> labelMap = new LinkedHashMap<>();
    /**待导出的文件夹*/
    private Map<String, UnsPo> exportFolderMap = new LinkedHashMap<>();
    private Set<Long> exportFolderId = new HashSet<>();

    /**待导出的文件*/
    private Map<String, UnsPo> exportFileMap = new LinkedHashMap<>();

    /**待导出的文件id集合*/
    private Map<Long, String> fileIdToAliasMap = new HashMap<>();
    /**待导出的文件path集合*/
    private Map<String, String> filePathToAliasMap = new HashMap<>();





    Map<Long, Set<String>> unsLabelNamesMap = new HashMap<>();

    /***************文件关联的引用文件****************/
    private Set<Long> checkReferIds = new HashSet<>();
    private Set<String> checkReferAliass = new HashSet<>();
    private Set<String> checkReferPaths = new HashSet<>();

    public ExcelExportContext(String language) {
        this.language = language;
    }

    public void putAllTemplate(Map<Long, UnsPo> templateMap) {
        this.templateMap.putAll(templateMap);
    }

    public void putAllLabels(Map<Long, UnsLabelPo> labelMap) {
        this.labelMap.putAll(labelMap);
    }

    public void computeIfAbsentLabel(Long unsId, String labelName) {
        unsLabelNamesMap.computeIfAbsent(unsId, k -> new HashSet<>()).add(labelName);
    }

    public void addExportFolder(UnsPo exportFolder) {
        exportFolderMap.put(exportFolder.getAlias(), exportFolder);
        exportFolderId.add(exportFolder.getId());
    }

    public boolean containExportFolder(Long folderId) {
        return exportFolderId.contains(folderId);
    }

    public Collection<UnsPo> getAllExportFolder() {
        return exportFolderMap.values();
    }

    public void addExportFile(UnsPo exportFile) {
        if (exportFileMap.put(exportFile.getAlias(), exportFile) == null) {
            fileIdToAliasMap.put(exportFile.getId(), exportFile.getAlias());
            filePathToAliasMap.put(exportFile.getPath(), exportFile.getAlias());
        }
    }

    public Collection<UnsPo> getAllExportFile() {
        return exportFileMap.values();
    }

    public UnsPo getExportFileById(Long id) {
        String alias = fileIdToAliasMap.get(id);
        if (alias != null) {
            return exportFileMap.get(alias);
        }
        return null;
    }

    public UnsPo getExportFileByPath(String path) {
        String alias = filePathToAliasMap.get(path);
        if (alias != null) {
            return exportFileMap.get(alias);
        }
        return null;
    }

    public UnsPo getExportFileByAlias(String alias) {
        return exportFileMap.get(alias);
    }

    /**
     * 导出时解析导出文件关联引用文件
     * @param refers
     */
    public void addCheckRefer(InstanceField[] refers) {
        if (refers != null && refers.length > 0) {
            for (InstanceField refer : refers) {
                if (refer.getId() != null) {
                    checkReferIds.add(refer.getId());
                } else if (StringUtils.isNotBlank(refer.getAlias())) {
                    checkReferAliass.add(refer.getAlias());
                } else if (StringUtils.isNotBlank(refer.getPath())) {
                    checkReferPaths.add(refer.getPath());
                }
            }
        }
    }
}
