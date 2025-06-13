package com.supos.common.adpater.historyquery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


public class Where {
    @Getter
    @Setter
    List<WhereCondition> and = new ArrayList<>(4);
    @Getter
    @Setter
    List<WhereCondition> or = new ArrayList<>(4);

    public boolean isEmpty() {
        return and.isEmpty() && or.isEmpty();
    }

    @JsonIgnore
    @Hidden
    public transient Duration timeRange;
}
