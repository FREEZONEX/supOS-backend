package com.supos.uns.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
@Data
@Schema(description = "流计算详情")
public class StreamDetail {
    @Schema(description = "命名空间", example = "/a/b")
    String namespace;// 命名空间
   @Schema(description = "描述", example = "my topic")
    String description;// 描述
   @Schema(description = "流计算创建语句", example = "create stream xx...")
    String sql;// 创建流的执行语句
   @Schema(description = "流计算状态：0--未知, 1--在运行, 2--已暂停", example = "2")
    int status;
   @Schema(description = "创建时间 时间戳，单位：毫秒", example = "1732180399111")
    long createTime;//创建时间 时间戳，单位：毫秒
}
