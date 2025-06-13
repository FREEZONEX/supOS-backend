package com.supos.uns.vo;

import com.supos.common.annotation.SQLExpressionValidator;
import com.supos.common.dto.AlarmRuleDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.utils.JsonUtil;
import com.supos.common.vo.UserManageVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;

@Data
@NoArgsConstructor
public class CreateAlarmRuleVo {
    @NotEmpty(message = "uns.alarm.emptyName") @Size(min = 1, max = 63, message = "uns.alarm.name.size") @Valid
    String name; //报警名称
    @Size(min = 1, max = 255, message = "uns.alarm.description.size") @Valid
    String description;// 描述
    @NotNull @Valid
    AlarmRuleDefine protocol;// //条件 + 限值 + 死区类型（1-值，2百分比） + 死去值 + 越限时长

    @NotEmpty @Size(min = 1, max = 1) @Valid
    InstanceField[] refers;// 计算实例引用的其他实例字段
    @SQLExpressionValidator(field = "expression")
    String expression;// 计算表达式
    @NotNull @Valid
    Integer withFlags;//接收方式 16-人员 32-工作流程
    LinkedHashMap<String,Object> extend;//扩展字段   workflow表主键ID
    // 接收方式为人员的用户集合 id & preferredUsername
    List<UserManageVo> userList;

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
