package com.supos.uns.service;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.camunda.po.ProcessDefinitionPo;
import com.supos.camunda.service.ProcessService;
import com.supos.camunda.service.ProcessTaskService;
import com.supos.common.Constants;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.AlarmRuleDefine;
import com.supos.common.dto.BaseResult;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.enums.SysModuleEnum;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import com.supos.common.vo.UserManageVo;
import com.supos.uns.dao.mapper.AlarmHandlerMapper;
import com.supos.uns.dao.mapper.AlarmMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.AlarmHandlerPo;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.AlarmConfirmVo;
import com.supos.uns.vo.AlarmQueryVo;
import com.supos.uns.vo.AlarmVo;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.supos.uns.service.UnsManagerService.genIdForPath;

@Service
public class AlarmService extends ServiceImpl<AlarmMapper, AlarmPo> {

    @Resource
    private TodoService todoService;
    @Resource
    private SystemConfig systemConfig;
    @Resource
    private AlarmMapper alarmMapper;
    @Resource
    private AlarmHandlerMapper alarmHandlerMapper;
    @Resource
    private UnsMapper unsMapper;
    @Resource
    private ProcessTaskService processTaskService;
    @Resource
    private ProcessService processService;

    public PageResultDTO<AlarmVo> pageList(AlarmQueryVo params) {
        Page<AlarmVo> page = new Page<>(params.getPageNo(), params.getPageSize());
        IPage<AlarmVo> iPage = this.baseMapper.pageList(page, params);
        PageResultDTO.PageResultDTOBuilder<AlarmVo> pageBuilder = PageResultDTO.<AlarmVo>builder()
                .total(iPage.getTotal()).pageNo(params.getPageNo()).pageSize(params.getPageSize());
        List<AlarmVo> list = iPage.getRecords();
        for (AlarmVo alarmVo : list) {
            AlarmRuleDefine define = new AlarmRuleDefine();
            define.parseExpression(alarmVo.getExpression());
            alarmVo.setCondition(define.getCondition());
            alarmVo.setCanHandler(isCanHandler(UserContext.get(),params.getTopic()));
        }
        return pageBuilder.code(0).data(iPage.getRecords()).build();
    }

    public boolean isCanHandler(UserInfoVo userInfoVo,String topic){
        if (userInfoVo == null){
            return false;
        }
        //超管 可处理
        if (userInfoVo.isSuperAdmin()){
            return true;
        }
        String instanceId = genIdForPath(topic);
        UnsPo instance = unsMapper.selectById(instanceId);
        if (null == instance){
            return false;
        }
        //人员
        if (checkWithFlags(instance.getWithFlags()) == Constants.UNS_FLAG_ALARM_ACCEPT_PERSON) {
            List<AlarmHandlerPo> handlerList = alarmHandlerMapper.getByTopic(topic);
            if (CollectionUtils.isEmpty(handlerList)){
                return false;
            }
            AlarmHandlerPo handlerPo = handlerList.stream().filter(h -> userInfoVo.getSub().equals(h.getUserId())).findFirst().orElse(null);
            return null != handlerPo;
        } else if (checkWithFlags(instance.getWithFlags()) == Constants.UNS_FLAG_ALARM_ACCEPT_WORKFLOW){
            //工作流查询流程中配置的人员
            ProcessDefinitionPo processDefinition = processService.getById(Long.valueOf(instance.getExtend()));
            if (null == processDefinition){
                return false;
            }
            List<UserTask> tasks = processTaskService.getUserTaskListByProcessDefinitionId(processDefinition.getProcessDefinitionId());
            if (CollectionUtils.isEmpty(tasks)){
                return false;
            }
            String userIdsStr = tasks.get(0).getCamundaCandidateUsers();
            List<String> userIds = StrUtil.split(userIdsStr,",");
            if (CollectionUtils.isEmpty(userIds)){
                return false;
            }
            String userId = userIds.stream().filter(user -> userInfoVo.getSub().equals(user)).findFirst().orElse(null);
            //如果存在返回true
            return StringUtils.isNotBlank(userId);
        }
        return false;
    }

    public BaseResult confirmAlarm(AlarmConfirmVo alarmConfirmVo){
        UserInfoVo userInfoVo = UserContext.get() != null ? UserContext.get() : new UserInfoVo(Constants.UNKNOWN_USER,Constants.UNKNOWN_USER);
        BaseResult result = new BaseResult(0, "ok");
        boolean isOk = true;
        if (alarmConfirmVo.getConfirmType() == 1){
            List<AlarmPo> list = alarmConfirmVo.getIds().stream().map(id -> {
                AlarmPo alarm = getById(id);
                alarm.setReadStatus(true);
                return alarm;
            }).collect(Collectors.toList());
            isOk = updateBatchById(list);
            handlerTodo(list,userInfoVo);
        } else if (alarmConfirmVo.getConfirmType() == 2) {
            List<AlarmPo> alarmList;
            if (userInfoVo.isSuperAdmin()){
                //超管可以处理，当前topic下所有未处理的报警记录
                alarmList = alarmMapper.getNoReadListByTopic(alarmConfirmVo.getTopic());
            } else {
                //普通用户 只能处理处理人为自己的未处理报警记录
               alarmList = alarmMapper.getNoReadListByTopicAndUserId(alarmConfirmVo.getTopic(),userInfoVo.getSub());
            }
            //报警设置为已读
            batchSetReadStatus(alarmList);
            //处理待办
            handlerTodo(alarmList,userInfoVo);
        }
        if (!isOk){
            result.setCode(500);
            result.setMsg(I18nUtils.getMessage("uns.alarm.confirm.failed"));
        }
        return result;
    }

    public void createAlarmHandler(String topic,List<UserManageVo> userList){
        if (CollectionUtils.isNotEmpty(userList)) {
            alarmHandlerMapper.delete(new LambdaQueryWrapper<AlarmHandlerPo>().eq(AlarmHandlerPo::getTopic, topic));
            alarmHandlerMapper.saveBatch(topic, userList);
        }
    }


    private void batchSetReadStatus(List<AlarmPo> alarmList){
        for (AlarmPo alarmPo : alarmList) {
            alarmPo.setReadStatus(true);
            updateById(alarmPo);
        }
    }

    /**
     * 处理待办
     * @param list
     * @param userInfoVo
     */
    private void handlerTodo(List<AlarmPo> list, UserInfoVo userInfoVo){
        ThreadUtil.execute(() -> {
            for (AlarmPo alarm : list) {
                String instanceId = genIdForPath(alarm.getTopic());
                todoService.handleTodo(SysModuleEnum.ALARM, instanceId,alarm.getId() + "",1,userInfoVo);
            }
        });
    }

    public static int checkWithFlags(int witchFlags) {
        if ((witchFlags & Constants.UNS_FLAG_ALARM_ACCEPT_PERSON) == 16) {
            return 16;
        } else if ((witchFlags & Constants.UNS_FLAG_ALARM_ACCEPT_WORKFLOW) == 32) {
            return 32;
        }
        return -1; // 如果都不包含，返回 -1
    }
}
