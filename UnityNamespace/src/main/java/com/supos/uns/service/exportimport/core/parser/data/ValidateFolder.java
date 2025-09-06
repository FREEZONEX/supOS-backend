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
import org.apache.commons.lang3.StringUtils;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ValidateFolder
 * @date 2025/2/20 19:11
 */
@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateFolder {

    private String flagNo;

    /**namespace*/
    @NotEmpty(message = "uns.topic.empty")
    @TopicNameValidator(message = "uns.folder.format.invalid")
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

    /**是否开启订阅*/
    //String subscribe;

    //String frequency;

    /**扩展属性*/
    String extendproperties;

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

    @Override
    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
