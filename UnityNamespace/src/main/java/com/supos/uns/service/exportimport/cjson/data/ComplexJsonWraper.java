package com.supos.uns.service.exportimport.cjson.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.supos.uns.service.exportimport.core.entity.FolderData;
import com.supos.uns.service.exportimport.core.entity.LabelData;
import com.supos.uns.service.exportimport.core.entity.TemplateData;
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
public class ComplexJsonWraper implements Serializable {

    @JsonProperty("Template")
    private List<TemplateData> templateDataList;

    @JsonProperty("Label")
    private List<LabelData> labelDataList;

    @JsonProperty("UNS")
    private List<FolderData> unsDataList;
}
