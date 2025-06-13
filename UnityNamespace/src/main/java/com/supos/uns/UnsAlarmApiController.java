package com.supos.uns;

import com.supos.common.dto.BaseResult;
import com.supos.common.dto.PageResultDTO;
import com.supos.uns.service.AlarmService;
import com.supos.uns.service.UnsAlarmService;
import com.supos.uns.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报警管理
 */
@RestController
@Slf4j
public class UnsAlarmApiController {

    @Autowired
    UnsAlarmService unsAlarmService;
    @Autowired
    AlarmService alarmService;

    @Operation(summary = "创建报警规则")
    @PostMapping(path = {"/inter-api/supos/uns/alarm/rule"})
    public BaseResult createAlarmRule(@Valid @RequestBody CreateAlarmRuleVo createAlarmRuleVo) {
        return unsAlarmService.createAlarmRule(createAlarmRuleVo);
    }

    @Operation(summary = "更新报警规则")
    @PutMapping(path = {"/inter-api/supos/uns/alarm/rule"})
    public BaseResult updateAlarmRule(@Valid @RequestBody UpdateAlarmRuleVo updateAlarmRuleVo) {
        return unsAlarmService.updateAlarmRule(updateAlarmRuleVo);
    }

    @Operation(summary = "查询报警列表")
    @PostMapping(path = {"/inter-api/supos/uns/alarm/pageList"})
    public PageResultDTO<AlarmVo> list(@Valid @RequestBody AlarmQueryVo params) {
        return alarmService.pageList(params);
    }

    @Operation(summary = "确认报警")
    @PostMapping(path = {"/inter-api/supos/uns/alarm/confirm"})
    public BaseResult confirmAlarm(@Valid @RequestBody AlarmConfirmVo alarmConfirmVo) {
        return alarmService.confirmAlarm(alarmConfirmVo);
    }
}
