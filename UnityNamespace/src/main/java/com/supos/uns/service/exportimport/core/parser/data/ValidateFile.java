package com.supos.uns.service.exportimport.core.parser.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.annotation.AliasValidator;
import com.supos.common.annotation.TopicNameValidator;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.PathUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class ValidateFile {

    private String flagNo;

    /**namespace*/
    @NotEmpty(message = "uns.topic.empty")
    @TopicNameValidator
    String path;

    /**别名*/
    @AliasValidator
    String alias;

    /**显示名*/
    String displayName;

    /**模板别名*/
    String templateAlias;

    /**字段定义*/
    String fields;

    /**描述*/
    String description;

    String dataType;

    String calculationType;

    String refers;

    String expression;

    String label;

    String frequency;

    String persistence;
    String autoDashboard;
    String mockData;

    /**
     * 0-3 see com.supos.common.enums.FolderDataType
     */
    Integer parentDataType;


    public void setPath(String path) {
        if (path != null && !path.isEmpty()) {
            path = path.trim();
        }
        this.path = path;
    }

    public CreateTopicDto createTopic() {
        CreateTopicDto topicDto = new CreateTopicDto();
        topicDto.setFlagNo(flagNo);
        topicDto.setPath(path);
        topicDto.setName(PathUtil.getName(path));
        topicDto.setAlias(alias);
        topicDto.setDisplayName(displayName);
        topicDto.setDescription(description);
        return topicDto;
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
