package com.supos.uns.service.exportimport.json.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: JsonWraper
 * @date 2025/5/10 10:01
 */
@Data
public class JsonWraper implements Serializable {

    @JsonProperty("Template")
    private List<TemplateData> templateDataList;

    @JsonProperty("Label")
    private List<LabelData> labelDataList;

    @JsonProperty("Folder")
    private List<FolderData> folderDataDataList;

    @JsonProperty("File-timeseries")
    private List<FileTimeseries> fileTimeseriesDataList;

    @JsonProperty("File-relation")
    private List<FileRelation> fileRelationDataList;

    @JsonProperty("File-calculate")
    private List<FileCalculate> fileCalculateDataList;

    @JsonProperty("File-aggregation")
    private List<FileAggregation> fileAggregationDataList;

    @JsonProperty("File-reference")
    private List<FileReference> fileReferenceDataList;
}
