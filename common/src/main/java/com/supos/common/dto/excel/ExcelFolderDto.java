package com.supos.common.dto.excel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.annotation.AliasValidator;
import com.supos.common.annotation.TopicNameValidator;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.PathUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/20 19:11
 */
@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExcelFolderDto {

    /**
     * 批次号
     */
    int batch;
    /**
     * 批次内序号
     */
    int index = -1;

    @NotEmpty(message = "uns.topic.empty")
    @TopicNameValidator(message = "uns.folder.format.invalid")
    String path;//主题

    //@NotEmpty(message = "uns.invalid.alias.empty")
    @AliasValidator
    String alias;//别名

    String displayName;//显示名

    String templateAlias;// 模板别名

    String fields;// 字段定义

    String description; //描述

    public CreateTopicDto createTopic() {
        CreateTopicDto topicDto = new CreateTopicDto();
        topicDto.setIndex(index);
        topicDto.setBatch(batch);
        topicDto.setPath(path);
        topicDto.setName(PathUtil.getName(path));
        topicDto.setAlias(alias);
        topicDto.setDisplayName(displayName);
        topicDto.setDescription(description);
        return topicDto;
    }

    public void trim() {
        if (StringUtils.isNotBlank(path)) {
            path = path.trim();
        }
        if (StringUtils.isNotBlank(alias)) {
            alias = alias.trim();
        }
        if (StringUtils.isNotBlank(displayName)) {
            displayName = displayName.trim();
        }
        if (StringUtils.isNotBlank(templateAlias)) {
            templateAlias = templateAlias.trim();
        }
        if (StringUtils.isNotBlank(description)) {
            description = description.trim();
        }
    }

    public String gainBatchIndex() {
        return String.format("%d-%d", batch, index);
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
