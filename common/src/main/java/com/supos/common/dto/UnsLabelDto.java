package com.supos.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnsLabelDto {


    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String labelName;

    private Integer withFlags;

    @Schema(description = "订阅频率")
    private String subscribeFrequency;

    @Schema(description = "订阅时间")
    private Date subscribeAt;

    List<Long> refUnsIds;

    private Date createAt;
    private Date updateAt;

    public UnsLabelDto(String labelName) {
        this.labelName = labelName;
    }

    public UnsLabelDto(Long id, String labelName) {
        this.id = id;
        this.labelName = labelName;
    }
}
