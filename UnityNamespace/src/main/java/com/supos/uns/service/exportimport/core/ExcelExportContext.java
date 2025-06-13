package com.supos.uns.service.exportimport.core;

import com.supos.common.dto.InstanceField;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelExportContext
 * @date 2025/4/23 16:40
 */
@Getter
public class ExcelExportContext {
    /**待导出的模板*/
    private Map<Long, UnsPo> templateMap = new HashMap<>();
    /**待导出的标签*/
    private Map<Long, UnsLabelPo> labelMap = new HashMap<>();
    /**待导出的文件夹*/
    private List<ExportNode> exportFolderList = new LinkedList<>();
    /**待导出的文件*/
    private List<ExportNode> exportFileList = new LinkedList<>();


    /**待导出的文件夹alias集合*/
    private Set<String> folderAlias = new HashSet<>();
    /**待导出的文件alias集合*/
    private Map<String, ExportNode> fileAliasMap = new HashMap<>();
    /**待导出的文件id集合*/
    private Map<Long, ExportNode> fileIdMap = new HashMap<>();
    /**待导出的文件path集合*/
    private Map<String, ExportNode> filePathMap = new HashMap<>();





    Map<Long, Set<String>> unsLabelNamesMap = new HashMap<>();

    private Set<Long> checkReferIds = new HashSet<>();
    private Set<String> checkReferAliass = new HashSet<>();
    private Set<String> checkReferPaths = new HashSet<>();

    public void putAllTemplate(Map<Long, UnsPo> templateMap) {
        this.templateMap.putAll(templateMap);
    }

    public void putAllLabels(Map<Long, UnsLabelPo> labelMap) {
        this.labelMap.putAll(labelMap);
    }

    public void computeIfAbsentLabel(Long unsId, String labelName) {
        unsLabelNamesMap.computeIfAbsent(unsId, k -> new HashSet<>()).add(labelName);
    }

    public void addExportFolder(ExportNode exportNode) {
        if (folderAlias.add(exportNode.getUnsPo().getAlias())) {
            exportFolderList.add(exportNode);
        }
    }

    public void addExportFile(ExportNode exportNode) {
        if (fileAliasMap.put(exportNode.getUnsPo().getAlias(), exportNode) == null) {
            exportFileList.add(exportNode);
            fileIdMap.put(exportNode.getUnsPo().getId(), exportNode);
            filePathMap.put(exportNode.getUnsPo().getPath(), exportNode);
        }
    }

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
