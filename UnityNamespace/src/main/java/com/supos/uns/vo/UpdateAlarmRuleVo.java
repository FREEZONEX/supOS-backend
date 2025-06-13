package com.supos.uns.vo;

import com.supos.common.utils.JsonUtil;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
public class UpdateAlarmRuleVo extends CreateAlarmRuleVo {

    @NotNull
    Long id;

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
