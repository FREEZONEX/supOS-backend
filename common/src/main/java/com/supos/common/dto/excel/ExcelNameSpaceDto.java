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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.apache.commons.lang3.StringUtils;

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
public class ExcelNameSpaceDto {
    /**
     * 批次号
     */
    int batch;
    /**
     * 批次内序号
     */
    int index = -1;
    @NotEmpty(message = "uns.topic.empty")
    @TopicNameValidator
    String path;//主题

    //@NotEmpty(message = "uns.invalid.alias.empty")
    @AliasValidator
    String alias;//别名

    String displayName;//显示名

    String templateAlias;// 模板别名

    String fields;// 字段定义

    String description; //描述

    String refers;

    String expression;

    public void setPath(String path) {
        if (path != null && !path.isEmpty()) {
            path = path.trim();
        }
        this.path = path;
    }

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

    public String gainBatchIndex() {
        return String.format("%d-%d", batch, index);
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
