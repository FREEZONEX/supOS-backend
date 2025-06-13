package com.supos.adpter.nodered.vo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class BatchQueryRequest {

    @NotNull
    private List<String> aliases;
}
