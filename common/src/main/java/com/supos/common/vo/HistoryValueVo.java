package com.supos.common.vo;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 历史查询结果响应对象
 */
@Data
@Schema(description = "历史查询结果响应对象")
public class HistoryValueVo {

    @Schema(description = "查询结果集合，每个元素代表一个文件的聚合查询结果")
    private List<Result> results;

    @Schema(description = "无授权的文件别名集合")
    private List<String> unauthorized;

    @Schema(description = "不存在的文件别名集合")
    private List<String> notExsistAtrributes;

    @Data
    @Schema(description = "单个文件的聚合查询结果")
    public static class Result {

        @Schema(description = "文件别名")
        private String alias;

        @Schema(description = "聚合函数，如 first、sum、mean 等")
        private String function;

        @Schema(description = "是否有下一页数据")
        private boolean hasNext;

        @Schema(description = "字段顺序，返回的数据字段顺序与此保持一致")
        private List<String> fields;

        @Schema(description = "数据集合，每条记录为一个 List，字段顺序与 fields 对应")
        private List<List<String>> datas;
    }
}
