package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.Constants;
import jakarta.validation.Valid;

@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateFileDto extends CreateTopicDto {

    public CreateFileDto() {
        setPathType(Constants.PATH_TYPE_FILE);
    }
}
