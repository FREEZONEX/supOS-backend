package com.supos.common.dto;

import com.alibaba.fastjson2.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 查询请求参数对象，用于构造查询条件。
 */
@Data
@Schema(description = "查询请求参数对象")
public class HistoryValueRequest {

    @Schema(description = "补点策略，可选值：none（不补点，默认），previous（使用前一个窗口值补点），line（线性补值）")
    private Fill fill;

    @Schema(description = "聚合窗口，格式为：窗口间隔[,窗口偏移]，单位支持：s秒、m分、h小时、d天，例如5s,1s")
    private GroupBy groupBy;

    @Schema(description = "返回的最大元素数目，默认1000，最大10000")
    private Integer limit = 1000;

    @Schema(description = "偏移量，从指定条数后开始查询，例如 offset=5 表示从第6条开始查询")
    private Integer offset = 0;

    @Schema(description = "排序方式，可选值：ASC（升序）、DESC（降序）")
    private String order = "desc";

    @Schema(description = "查询的字段表达式列表，例如：文件别名 'SAxxx' 或聚合函数 'first(\"SBxxx\")'  支持文件的指定字段查询：'文件.字段名'")
    private List<String> select;

    @Schema(description = "过滤条件")
    private JSONObject where;

    @Data
    @Schema(description = "补点策略对象")
    public static class Fill {
        @Schema(description = "补点策略，支持：none、previous、line")
        private String strategy;
    }

    @Data
    @Schema(description = "聚合窗口配置对象")
    public static class GroupBy {
        @Schema(description = "窗口配置字符串，如 5s,1s")
        private String time;
    }
}
