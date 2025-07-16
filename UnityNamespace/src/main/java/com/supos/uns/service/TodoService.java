package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.Constants;
import com.supos.common.dto.*;
import com.supos.common.enums.SysModuleEnum;
import com.supos.common.event.AlertEvent;
import com.supos.common.event.TopicMessageEvent;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.SqlUtil;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import com.supos.common.vo.UserManageVo;
import com.supos.uns.dao.mapper.AlarmHandlerMapper;
import com.supos.uns.dao.mapper.TodoMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.mapper.UserManageMapper;
import com.supos.uns.dao.po.AlarmHandlerPo;
import com.supos.uns.dao.po.TodoPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.CreateTodoVo;
import com.supos.uns.vo.TodoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TodoService extends ServiceImpl<TodoMapper, TodoPo> {

    @Resource
    private UnsMapper unsMapper;
    @Resource
    private TodoMapper todoMapper;
    @Resource
    private AlarmHandlerMapper alarmHandlerMapper;
    @Resource
    private UserManageMapper userManageMapper;

    @EventListener(classes = AlertEvent.class)
    public void alarmEvent(AlertEvent event) {
        if (event.dataType != Constants.ALARM_RULE_TYPE) {
            return;
        }
        log.info(">>>>>>>>>>>>处理TopicMessageEvent 报警数据,topic:{},data:{}", event.topic, event.data);
        //报警数据
        Map<String, Object> data = event.data;
        //报警规则
        Long instanceId = event.unsId;
        UnsPo instance = unsMapper.selectById(instanceId);
        if (null == instance) {
            log.warn(">>>>>>>>>>>>报警规则：{}，查询失败，待办不生成.0", event.topic);
            return;
        }
        List<UserManageVo> userList = null;
        //TODO
        Long processId = 1L;
//        Long processId = instance.getExtend() != null ? Long.valueOf(instance.getExtend()) : null;
        String processInstanceId = null;
        //如果是工作流
/*        if (AlarmService.checkWithFlags(instance.getWithFlags()) == Constants.UNS_FLAG_ALARM_ACCEPT_WORKFLOW) {
            //查询工作流所配置的用户  给每个用户发送待办
            ProcessDefinitionPo processDefinitionPo = processService.getById(processId);
            if (null == processDefinitionPo) {
                log.warn(">>>>>>>>>>>>报警规则：{}，获取工作流流程为空processId：{}，待办不生成.0", instance.getPath(), processId);
                return;
            }
            List<UserTask> tasks = processTaskService.getUserTaskListByProcessDefinitionId(processDefinitionPo.getProcessDefinitionId());
            if (CollectionUtils.isEmpty(tasks)) {
                log.warn(">>>>>>>>>>>>报警规则：{}，获取工作流处理人员列表为空，待办不生成.1", instance.getPath());
                return;
            }
            String userIdsStr = tasks.get(0).getCamundaCandidateUsers();
            List<String> userIds = StrUtil.split(userIdsStr, ",");
            userList = userManageMapper.listUserById(userIds);
            if (CollectionUtils.isEmpty(userList)) {
                log.warn(">>>>>>>>>>>>报警规则：{}，获取工作流处理人员列表为空，待办不生成.2", instance.getPath());
                return;
            }
            //启动流程实例
            ProcessInstanceVo processInstanceVo = processTaskService.startProcess(processId, null);
            processInstanceId = processInstanceVo.getProcessInstanceId();
        } else*/ if (AlarmService.checkWithFlags(instance.getWithFlags()) == Constants.UNS_FLAG_ALARM_ACCEPT_PERSON) {
            //接收类型：人员
            //获取报警规则配置的处理人列表
            List<AlarmHandlerPo> handlerList = alarmHandlerMapper.getByUnsId(instanceId);
            if (CollectionUtils.isEmpty(handlerList)) {
                log.warn(">>>>>>>>>>>>报警规则：{}，获取处理人员列表为空，待办不生成.", instance.getPath());
                return;
            }
            userList = handlerList.stream().map(h -> new UserManageVo(h.getUserId(), h.getUsername())).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(userList)) {
            log.warn(">>>>>>>>>>>>报警规则：{}，获取报警对应的处理人员为空，待办不生成.", instance.getPath());
            return;
        }

        InstanceField instanceField = instance.getRefers()[0];
        UnsPo uns = unsMapper.getById(instanceField.getId());
        if (uns == null) {
            log.warn(">>>>>>>>>>>>报警规则：{}，获取报警对应的文件失败，待办不生成.", instance.getPath());
            return;
        }
        AlarmRuleDefine alarmRuleDefine = new AlarmRuleDefine();
        alarmRuleDefine.parseExpression(instance.getExpression());
        long time = (long) data.get("_ct");
        String date = "${" + time + "}";
//        String date = DateUtil.format(new Date(time), "yyyy/MM/dd HH:mm:ss");
        boolean isAlarm = (boolean) data.get("is_alarm");
        String msg;
        if (isAlarm) {
            //【实例】,【属性】,【时间】 ,【条件】,【阀值】,【当前值】
            msg = I18nUtils.getMessage("todo.template.alarm",
                    uns.getPath(),
                    instanceField.getField(),
                    date,
                    alarmRuleDefine.getCondition(),
                    data.get("limit_value"),
                    data.get("current_value"));
        } else {
            msg = I18nUtils.getMessage("todo.template.alarm.cancel",
                    uns.getPath(),
                    instanceField.getField(),
                    date,
                    data.get("current_value"));
        }
        Date now = new Date();
        //待办表生成数据
        for (UserManageVo user : userList) {
            TodoPo todo = new TodoPo();
            todo.setUserId(user.getId());
            todo.setUsername(user.getPreferredUsername());
            todo.setModuleCode(SysModuleEnum.ALARM.getCode());
            todo.setModuleName(I18nUtils.getMessage(SysModuleEnum.ALARM.getCode()));
            todo.setStatus(0);
            todo.setTodoMsg(msg);
            todo.setBusinessId(instanceId);//实例ID
            todo.setLink(data.get("_id").toString());//报警ID
            todo.setCreateAt(now);
            todo.setProcessId(processId);
            todo.setProcessInstanceId(processInstanceId);
            save(todo);
        }
        log.debug(">>>>>>>>>>>>报警规则：{}，完成待办生成.", instance.getPath());
    }

    public PageResultDTO<TodoVo> pageList(TodoQueryDto todoQueryDto) {
        Page<TodoPo> page = new Page<>(todoQueryDto.getPageNo(), todoQueryDto.getPageSize(), true);
        TodoQuery query = BeanUtil.copyProperties(todoQueryDto, TodoQuery.class);
        query.setTodoMsg(SqlUtil.escapeForLike(query.getTodoMsg()));
        UserInfoVo user = UserContext.get();
//        UserInfoVo user = new UserInfoVo("7a85853b-a913-443d-9a3e-5e3a84e455d0","xwj");
        if (user == null) {
            //未登录，返回空
            return PageResultDTO.<TodoVo>builder().code(200)
                    .pageNo(todoQueryDto.getPageNo())
                    .pageSize(todoQueryDto.getPageSize())
                    .data(Collections.emptyList()).build();
        }
        String username = user.getPreferredUsername();
        LambdaQueryWrapper<TodoPo> qw = new LambdaQueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(todoQueryDto.getModuleCode()), TodoPo::getModuleCode, todoQueryDto.getModuleCode());
        qw.eq(todoQueryDto.getStatus() != null, TodoPo::getStatus, todoQueryDto.getStatus());
        qw.like(StringUtils.isNotBlank(todoQueryDto.getTodoMsg()), TodoPo::getTodoMsg, todoQueryDto.getTodoMsg());

        if (user.isSuperAdmin()) {
            if (Boolean.TRUE.equals(todoQueryDto.getMyTodo())) {
                qw.eq(TodoPo::getHandlerUsername, username);
            }
        } else {
            //普通用户
            if (Boolean.TRUE.equals(todoQueryDto.getMyTodo())) {
                qw.eq(TodoPo::getHandlerUsername, username);
            } else {
                //已办tab  未勾选我的已办 = 已办人或者接收人是自己的记录
                qw.and(wrapper -> wrapper
                        .eq(TodoPo::getUsername, username)
                        .or()
                        .eq(TodoPo::getHandlerUsername, username));
            }
        }

        IPage<TodoPo> iPage = todoMapper.selectPage(page, qw);
        List<TodoVo> voList = iPage.getRecords().stream().map(todo -> {
            TodoVo vo = BeanUtil.copyProperties(todo, TodoVo.class);
            return vo;
        }).collect(Collectors.toList());
        PageResultDTO.PageResultDTOBuilder<TodoVo> pageBuilder = PageResultDTO.<TodoVo>builder()
                .total(iPage.getTotal()).pageNo(todoQueryDto.getPageNo()).pageSize(todoQueryDto.getPageSize());
        return pageBuilder.code(200).data(voList).build();
    }

    public PageResultDTO<TodoVo> pageListByOpen(TodoOpenQueryDto todoQueryDto) {
        TodoQuery query = BeanUtil.copyProperties(todoQueryDto, TodoQuery.class);
        query.setTodoMsg(SqlUtil.escapeForLike(query.getTodoMsg()));
        Page<TodoPo> page = new Page<>(todoQueryDto.getPageNo(), todoQueryDto.getPageSize(), true);
        IPage<TodoPo> iPage = todoMapper.pageList(page, query);
        List<TodoVo> voList = iPage.getRecords().stream().map(todo -> {
            TodoVo vo = BeanUtil.copyProperties(todo, TodoVo.class);
            return vo;
        }).collect(Collectors.toList());
        PageResultDTO.PageResultDTOBuilder<TodoVo> pageBuilder = PageResultDTO.<TodoVo>builder()
                .total(iPage.getTotal()).pageNo(todoQueryDto.getPageNo()).pageSize(todoQueryDto.getPageSize());
        return pageBuilder.code(200).data(voList).build();
    }

    public ResultVO handler(HandleTodoDto handleTodoDto) {
        TodoPo todoPo = todoMapper.selectById(handleTodoDto.getId());
        if (todoPo == null) {
            return ResultVO.fail("对应的待办不存在");
        }

        if (todoPo.getStatus() == 1){
            return ResultVO.fail("此待办已处理完成，不允许再次被处理");
        }

        UserManageVo user = userManageMapper.getByUsername(handleTodoDto.getUsername());
        if (user == null) {
            return ResultVO.fail("用户信息不存在");
        }
        todoPo.setStatus(1);
        todoPo.setHandlerUsername(user.getPreferredUsername());
        todoPo.setHandlerUserId(user.getId());
        todoPo.setHandlerTime(new Date());
        todoMapper.updateById(todoPo);
        return ResultVO.success("ok");
    }

    public boolean handleTodo(SysModuleEnum module, Long businessId, String link, int status, UserInfoVo userInfoVo) {
        //如果是报警模块
//        if (SysModuleEnum.ALARM.equals(module)) {
//            UnsPo instance = unsMapper.selectById(businessId);
//            if (null == instance) {
//                log.warn("报警规则ID:{},报警规则不存在,工作流领取并完成任务跳过", businessId);
//            } else {
//                if (AlarmService.checkWithFlags(instance.getWithFlags()) == Constants.UNS_FLAG_ALARM_ACCEPT_WORKFLOW) {
//                    //如果是配置了工作流，查询工作流的实例ID  领取并完成任务
//                    LambdaQueryWrapper<TodoPo> qw = new LambdaQueryWrapper<>();
//                    qw.eq(TodoPo::getModuleCode, module.getCode()).eq(TodoPo::getBusinessId, businessId)
//                            .eq(TodoPo::getLink, link).eq(TodoPo::getUserId, userInfoVo.getSub()).last("limit 1");
//                    TodoPo todoPo = this.baseMapper.selectOne(qw);
//                    String processInstanceId = todoPo.getProcessInstanceId();
//                    //领取并完成任务
//                    processTaskService.claimAndCompleteTask(processInstanceId, userInfoVo.getSub(), null);
//                }
//            }
//        }
        return todoMapper.updateTodoStatus(module.getCode(), businessId, status, userInfoVo.getPreferredUsername(), userInfoVo.getSub(), link) > 0;
    }

    public ResultVO createTodo(CreateTodoVo createTodoVo) {
        String username = createTodoVo.getUsername();
        UserManageVo user = userManageMapper.getByUsername(username);
        if (user == null) {
            return ResultVO.fail("用户信息不存在");
        }
        TodoPo todoPo = new TodoPo();
        todoPo.setUsername(user.getPreferredUsername());
        todoPo.setUserId(user.getId());
        todoPo.setModuleCode(createTodoVo.getModuleCode());
        todoPo.setModuleName(createTodoVo.getModuleName());
        todoPo.setTodoMsg(createTodoVo.getTodoMsg());
        todoPo.setLink(createTodoVo.getLink());
        save(todoPo);
        return ResultVO.success("ok");
    }

    public ResultVO<SysModuleDto> getModuleList() {
        List<TodoPo> list = todoMapper.getModuleList();
        List<SysModuleDto> moduleList = list.stream().map(todo -> {
            SysModuleDto module = new SysModuleDto();
            module.setModuleCode(todo.getModuleCode());
            module.setModuleName(todo.getModuleName());
            return module;
        }).collect(Collectors.toList());
        return ResultVO.successWithData(moduleList);
    }
}
