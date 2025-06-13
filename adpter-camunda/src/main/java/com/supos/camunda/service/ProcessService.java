package com.supos.camunda.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.camunda.dto.CreateProcessDefinition;
import com.supos.camunda.dto.ProcessQueryDto;
import com.supos.camunda.mapper.ProcessDefinitionMapper;
import com.supos.camunda.po.ProcessDefinitionPo;
import com.supos.camunda.vo.ProcessDefinitionVo;
import com.supos.camunda.vo.ProcessInstanceVo;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/28 9:20
 */
@Slf4j
@Service
public class ProcessService extends ServiceImpl<ProcessDefinitionMapper,ProcessDefinitionPo> {

    @Resource
    private RepositoryService repositoryService;
    @Resource
    private ProcessTaskService processTaskService;


    /**
     * 流程定义分页列表
     * @param params
     * @return
     */
    public PageResultDTO<ProcessDefinitionVo> pageList(ProcessQueryDto params){
        Page<ProcessDefinitionPo> page = new Page<>(params.getPageNo(), params.getPageSize(),true);
        LambdaQueryWrapper<ProcessDefinitionPo> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.isNotBlank(params.getDescription()),ProcessDefinitionPo::getDescription,params.getDescription());
        if (StringUtils.isNotBlank(params.getProcessDefinitionName())){
            qw.apply("process_definition_name ILIKE CONCAT('%', {0}, '%')", params.getProcessDefinitionName());
        }
        Page<ProcessDefinitionPo> iPage = this.baseMapper.selectPage(page,qw);
        List<ProcessDefinitionVo> voList = iPage.getRecords().stream().map(po ->{
            ProcessDefinitionVo vo = BeanUtil.copyProperties(po,ProcessDefinitionVo.class);
            if (StringUtils.isNotBlank(po.getProcessDefinitionId())){
                vo.setInstanceList(processTaskService.getInstanceListByDefinitionId(po.getProcessDefinitionId()));
            }
            return vo;
        }).collect(Collectors.toList());
        PageResultDTO.PageResultDTOBuilder<ProcessDefinitionVo> pageBuilder = PageResultDTO.<ProcessDefinitionVo>builder()
                .total(iPage.getTotal()).pageNo(params.getPageNo()).pageSize(params.getPageSize());
        return pageBuilder.code(200).data(voList).build();
    }

    /**
     * 创建或更新流程
     * @param createProcess
     * @return
     */
    public ResultVO createOrUpdate(CreateProcessDefinition createProcess){
        boolean isCreate = createProcess.getId() == null;
        if (isCreate){
            ProcessDefinitionPo po = new ProcessDefinitionPo();
            po.setProcessDefinitionName(createProcess.getName());
            po.setDescription(createProcess.getDescription());
            save(po);
        } else {
            LambdaUpdateWrapper<ProcessDefinitionPo> qw = new LambdaUpdateWrapper<>();
            qw.eq(ProcessDefinitionPo::getId, createProcess.getId());
            qw.set(ProcessDefinitionPo::getProcessDefinitionName, createProcess.getName());
            qw.set(ProcessDefinitionPo::getDescription, createProcess.getDescription());
            update(qw);
        }
        return ResultVO.success("ok");
    }

    /**
     * 部署
     * @param id
     * @param inputStreamSource
     * @return
     * @throws IOException
     */
    public ResultVO<ProcessDefinitionVo> deploy(Long id, InputStreamSource inputStreamSource) throws IOException {
        ProcessDefinitionPo processDefinitionPo = getById(id);
        if (null == processDefinitionPo){
            return ResultVO.fail(I18nUtils.getMessage("workflow.process.definition.not.exists"));
        }
        String xml = IoUtil.read(inputStreamSource.getInputStream(), CharsetUtil.CHARSET_UTF_8);
        BpmnModelInstance bpmnModelInstance = Bpmn.readModelFromStream(inputStreamSource.getInputStream());
        Deployment deployment = repositoryService.createDeployment()
                .addModelInstance(processDefinitionPo.getProcessDefinitionName() + ".bpmn",bpmnModelInstance)
                .name(processDefinitionPo.getProcessDefinitionName())
                .deploy();
        log.info("processDefinitionName:{},部署成功，部署 ID：{}",processDefinitionPo.getProcessDefinitionName(),deployment.getId());
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId()).singleResult();

        processDefinitionPo.setProcessDefinitionId(processDefinition.getId());
        processDefinitionPo.setProcessDefinitionName(processDefinition.getName());
        processDefinitionPo.setProcessDefinitionKey(processDefinition.getKey());
        processDefinitionPo.setDeployId(processDefinition.getDeploymentId());
        processDefinitionPo.setDeployName(processDefinition.getResourceName());
        processDefinitionPo.setDeployTime(deployment.getDeploymentTime());
        processDefinitionPo.setBpmnXml(xml);
        //状态设置为启动
        processDefinitionPo.setStatus(1);
        updateById(processDefinitionPo);
        ProcessDefinitionVo vo = BeanUtil.copyProperties(processDefinitionPo,ProcessDefinitionVo.class);
        return ResultVO.successWithData(vo);
    }

    /**
     * 暂停流程
     * @param id
     * @return
     */
    public ResultVO stop(Long id){
        ProcessDefinitionPo processDefinitionPo = getById(id);
        if (null == processDefinitionPo){
            return ResultVO.fail(I18nUtils.getMessage("workflow.process.definition.not.exists"));
        }
        List<ProcessInstanceVo> instanceList = processTaskService.getInstanceListByDefinitionId(processDefinitionPo.getProcessDefinitionId());
        if (!CollectionUtils.isEmpty(instanceList)){
            return ResultVO.fail(I18nUtils.getMessage("workflow.process.definition.stop.failed"));
        }
        processDefinitionPo.setStatus(3);
        updateById(processDefinitionPo);
        return ResultVO.success("ok");
    }

    /**
     * 流程定义列表  全部
     * @return
     */
    public List<ProcessDefinitionVo> processDefinitionList() {
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        List<ProcessDefinition> processDefinitions = processDefinitionQuery.latestVersion().list();
        return processDefinitions.stream().map(this::convert2Vo).collect(Collectors.toList());
    }

    public ProcessDefinitionVo queryById(String processDefinitionId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        ProcessDefinitionVo pd = convert2Vo(processDefinition);
        byte[] bytes = IoUtil.readBytes(repositoryService.getProcessModel(processDefinitionId));
        String xmlContent = new String(bytes, StandardCharsets.UTF_8);
        pd.setBpmnXml(xmlContent);
        return pd;
    }

    public List<ProcessDefinition> queryByIds(Collection<String> ids) {
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        return processDefinitionQuery.processDefinitionIdIn(ids.toArray(new String[ids.size()])).list();
    }

    private void queryProcessDefinition(String id){
        //查询流程是否被挂起
        repositoryService.updateProcessDefinitionSuspensionState().byProcessDefinitionId(id).activate();
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(id);
        boolean isSuspended = processDefinition.isSuspended();
    }


    private ProcessDefinitionVo convert2Vo(ProcessDefinition processDefinition){
        ProcessDefinitionVo pd = new ProcessDefinitionVo();
        pd.setProcessDefinitionId(processDefinition.getId());
        pd.setProcessDefinitionName(processDefinition.getName());
        pd.setProcessDefinitionKey(processDefinition.getKey());
        pd.setDeployId(processDefinition.getDeploymentId());
        pd.setDeployName(processDefinition.getResourceName());
        return pd;
    }

    private ProcessDefinitionPo convert2Po(ProcessDefinition processDefinition){
        ProcessDefinitionPo pd = new ProcessDefinitionPo();
        pd.setProcessDefinitionId(processDefinition.getId());
        pd.setProcessDefinitionName(processDefinition.getName());
        pd.setProcessDefinitionKey(processDefinition.getKey());
        pd.setDeployId(processDefinition.getDeploymentId());
        pd.setDeployName(processDefinition.getResourceName());
        return pd;
    }


    //挂起一个流程
//        repositoryService.updateProcessDefinitionSuspensionState().byProcessDefinitionId()

}
