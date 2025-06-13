package com.supos.uns.vo;

import com.supos.common.dto.PaginationDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlarmQueryVo extends PaginationDTO {


    private Long unsId; // 报警规则ID = unsId

    /**
     * 是否已读
     */
    private Boolean readStatus;

}
