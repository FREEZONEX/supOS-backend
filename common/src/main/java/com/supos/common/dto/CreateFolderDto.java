package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.Constants;
import jakarta.validation.Valid;

@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateFolderDto extends CreateTopicDto {

    public CreateFolderDto() {
        setPathType(Constants.PATH_TYPE_DIR);
    }
}
