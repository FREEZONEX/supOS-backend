package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.Constants;
import com.supos.common.vo.LabelVo;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CreateFileDto extends CreateTopicDto {

    List<LabelVo> labelList;

    public CreateFileDto() {
        setPathType(Constants.PATH_TYPE_FILE);
    }
}
