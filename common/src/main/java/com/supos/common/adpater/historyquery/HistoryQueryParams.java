package com.supos.common.adpater.historyquery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supos.common.annotation.HistoryQueryParamsValidator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 历史查询参数
 */
@Getter
@Setter
@HistoryQueryParamsValidator
public class HistoryQueryParams {
    FillStrategy fillStrategy = FillStrategy.None;
    IntervalWindow intervalWindow;//聚合窗口，配合聚合函数使用
    @Max(value = 10000, message = "uns.hist.limit.max")
    @Min(value = 1, message = "uns.hist.limit.min")
    int limit = 1000;//返回的元素数目（默认1000），最大10000条
    @Min(value = 0, message = "uns.hist.offset.min")
    long offset;//偏移量,从指定条数后开始查询
    boolean ascOrder;// 按时间升序或降序
    @NotEmpty(message = "uns.hist.select.empty")
    Select[] select;// 选择的表列：如 aliasA.value, first(b.value)
    // 当 select 包含聚合函数， 且 intervalWindow 为空，则where时间条件不能是单向（如只有>或只有<=)的（此时需要按时间范围 / limit 估算时间窗口)
    @NotNull(message = "uns.hist.where.empty")
    Where where;

    @JsonIgnore
    transient int samples;//采样数量
    @JsonIgnore
    transient long minTime;// 时间范围中的最小时间
    @JsonIgnore
    transient long maxTime;// 时间范围中的最大时间
}
