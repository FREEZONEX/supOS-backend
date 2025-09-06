package com.supos.uns.service.exportimport.core.parser.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.utils.JsonUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
public class ValidateTemplate {

    private String flagNo;

    /**名称*/
    @NotEmpty
    @Size(max = 63)
    String name;

    /**别名*/
    String alias;

    /**字段定义*/
    String fields;

    /**描述*/
    String description;

    /**是否开启订阅*/
    //String subscribe;

    //String frequency;

    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            name = name.trim();
        }
        this.name = name;
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
