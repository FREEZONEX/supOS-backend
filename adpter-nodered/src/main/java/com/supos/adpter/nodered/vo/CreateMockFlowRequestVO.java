package com.supos.adpter.nodered.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

@Data
@Valid
public class CreateMockFlowRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    @NotEmpty(message = "alias can't be empty")
    private String unsAlias;

    @NotEmpty(message = "path can't be empty")
    private String path;

}
