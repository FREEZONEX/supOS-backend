package com.supos.uns.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class AlarmConfirmVo  {

    /**
     * 确认类型 1-批量确认 2-全部确认
     */
    @NotNull
    private Integer confirmType;

    private Long unsId;

    /**
     * 是否已读
     */
    private List<Long> ids;

}
