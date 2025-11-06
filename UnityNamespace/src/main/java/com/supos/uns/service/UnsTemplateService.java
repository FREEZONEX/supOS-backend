package com.supos.uns.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.enums.ActionEnum;
import com.supos.common.enums.EventMetaEnum;
import com.supos.common.enums.ServiceEnum;
import com.supos.common.event.EventBus;
import com.supos.common.event.SysEvent;
import com.supos.common.event.UpdateInstanceEvent;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.*;
import com.supos.uns.dao.mapper.AlarmMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.dto.WebhookDataDTO;
import com.supos.uns.util.PageUtil;
import com.supos.uns.util.UnsConverter;
import com.supos.uns.util.WebhookUtils;
import com.supos.uns.vo.CreateTemplateVo;
import com.supos.uns.vo.FileVo;
import com.supos.uns.vo.RemoveResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.supos.common.utils.FieldUtils.countNumericFields;

@Service
@Slf4j
public class UnsTemplateService extends ServiceImpl<UnsMapper, UnsPo> {

    @Autowired
    UnsRemoveService unsRemoveService;
    @Autowired
    UnsCalcService unsCalcService;
    @Autowired
    UnsMapper unsMapper;
    @Autowired
    AlarmMapper alarmMapper;
    @Autowired
    UnsDefinitionService unsDefinitionService;
    @Autowired
    UnsQueryService unsQueryService;

    private static final long TEMPLATE_ROOT_id = 1L;
    private static final String TEMPLATE_ROOT_ALIAS = "__templates__";
    private static final String TEMPLATE_ROOT_NAME = "tmplt";

    public ResultVO<String> createTemplate(CreateTemplateVo createTemplateVo) {
        String name = createTemplateVo.getName();
        String alias = createTemplateVo.getAlias();
        List<UnsPo> unsPos = list(new LambdaQueryWrapper<UnsPo>().eq(UnsPo::getAlias, alias));
        if (!CollectionUtils.isEmpty(unsPos)) {
            ResultVO resultVO = new ResultVO<>();
            resultVO.setCode(400);
            resultVO.setMsg(I18nUtils.getMessage("uns.template.alias.already.exists"));
            resultVO.setData(unsPos.get(0).getId().toString());
            return resultVO;
        }

        List<String> allTemplateNames = unsDefinitionService.allDefinitions().stream().filter(dto -> Constants.PATH_TYPE_TEMPLATE == dto.getPathType())
                .map(CreateTopicDto::getName).collect(Collectors.toList());
        name = PathUtil.generateUniqueName(name, allTemplateNames);

        String fieldError = FieldUtils.validateFields(createTemplateVo.getFields(), true);
        if (StringUtils.hasText(fieldError)) {
            return ResultVO.fail(fieldError);
        }

        UnsPo unsPo = new UnsPo();
        unsPo.setId(SuposIdUtil.nextId());
        unsPo.setName(name);
        setTemplateParent(unsPo);
        unsPo.setDataType(0);
        unsPo.setFields(createTemplateVo.getFields());
        unsPo.setDescription(createTemplateVo.getDescription());
        unsPo.setAlias(alias);
        save(unsPo);
        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(Collections.singletonList(unsPo));
        if (!webhookData.isEmpty()) {
            EventBus.publishEvent(new SysEvent(this, ServiceEnum.UNS_SERVICE, EventMetaEnum.UNS_FILED_CHANGE,
                    ActionEnum.ADD, webhookData));
        }
        return ResultVO.successWithData(unsPo.getId().toString());
    }

    private static void setTemplateParent(UnsPo unsPo) {
        unsPo.setDataType(0);
        unsPo.setPathType(Constants.PATH_TYPE_TEMPLATE);
        unsPo.setPath(TEMPLATE_ROOT_NAME + "/" + unsPo.getName());
        unsPo.setParentId(TEMPLATE_ROOT_id);
        unsPo.setParentAlias(TEMPLATE_ROOT_ALIAS);
        unsPo.setLayRec(TEMPLATE_ROOT_id + "/" + unsPo.getId().toString());
    }

    @Transactional(timeout = 300, rollbackFor = Throwable.class)
    public Map<String, String> createTemplates(Collection<CreateTemplateVo> createTemplateVos) {
        Set<String> aliasSet = createTemplateVos.stream().map(CreateTemplateVo::getAlias)
                .collect(Collectors.toSet());
        List<UnsPo> existTemplates = list(
                Wrappers.lambdaQuery(UnsPo.class).in(UnsPo::getAlias, aliasSet));
        Map<String, UnsPo> existTemplateMap = existTemplates.stream()
                .collect(Collectors.toMap(UnsPo::getAlias, Function.identity(), (k1, k2) -> k2));

        Map<String, String> errorMap = new HashMap<>();
        List<UnsPo> unsPos = new ArrayList<>(createTemplateVos.size());
        for (CreateTemplateVo createTemplateVo : createTemplateVos) {
            String alias = createTemplateVo.getAlias();
            if (existTemplateMap.containsKey(alias)) {
                errorMap.put(createTemplateVo.gainBatchIndex(),
                        I18nUtils.getMessage("uns.template.alias.already.exists"));
                continue;
            }
            UnsPo unsPo = new UnsPo(createTemplateVo.getName());
            unsPo.setId(SuposIdUtil.nextId());
            unsPo.setName(createTemplateVo.getName());

            setTemplateParent(unsPo);

            unsPo.setDataType(0);
            unsPo.setFields(createTemplateVo.getFields());
            unsPo.setDescription(createTemplateVo.getDescription());
            unsPo.setAlias(alias);
            unsPos.add(unsPo);
        }

        if (!CollectionUtils.isEmpty(unsPos)) {
            saveBatch(unsPos);
        }
        return errorMap;
    }

    public RemoveResult deleteTemplate(Long id) {
        UnsPo template = getById(id);
        if (null == template) {
            RemoveResult result = new RemoveResult();
            result.setCode(404);
            result.setMsg(I18nUtils.getMessage("uns.template.not.exists"));
            return result;
        }
        QueryWrapper<UnsPo> queryWrapper = new QueryWrapper<UnsPo>().eq("model_id", id);
        List<UnsPo> unsPos = this.list(queryWrapper);
        List<Long> folderIds = new ArrayList<>(32);
        List<UnsPo> files = new ArrayList<>(unsPos.size());
        for (UnsPo po : unsPos) {
            int pathType = po.getPathType();
            switch (pathType) {
                case Constants.PATH_TYPE_FILE: {
                    files.add(po);
                    break;
                }
                case Constants.PATH_TYPE_DIR: {
                    folderIds.add(po.getId());
                    break;
                }
            }
        }
        files.add(template);
        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(unsPos);
        if (!webhookData.isEmpty()) {
            EventBus.publishEvent(new SysEvent(this, ServiceEnum.UNS_SERVICE, EventMetaEnum.UNS_FILED_CHANGE,
                    ActionEnum.DELETE, webhookData));
        }

        if (!CollectionUtils.isEmpty(folderIds)) {
            LambdaUpdateWrapper<UnsPo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(UnsPo::getId, folderIds);
            updateWrapper.set(UnsPo::getModelId, null);
            update(updateWrapper);
        }
        List<CreateTopicDto> dtoList = files.stream().map(p -> UnsConverter.po2dto(p, false)).toList();

        return unsRemoveService.getRemoveResult(true, true, true, dtoList);
    }

    public ResultVO subscribeModel(Long id, Boolean enable, String frequency) {
        if (enable == null && frequency == null) {
            return ResultVO.fail("enable and frequency not be null");
        }

        UnsPo template = getById(id);
        if (null == template) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }

        LambdaUpdateWrapper<UnsPo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UnsPo::getId, id);
        if (enable != null) {
            Integer flags = template.getWithFlags();
            if (flags == null) {
                flags = 0;
            }
            flags = enable ? (flags | Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE) : (flags & ~Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE);
            updateWrapper.set(UnsPo::getWithFlags, flags);
            if (enable) {
                updateWrapper.set(UnsPo::getSubscribeAt, new Date());
            }
        }
        if (frequency != null) {
            Map<String, Object> protocol = new HashMap<>();
            protocol.put("frequency", frequency);
            updateWrapper.set(StringUtils.hasText(frequency), UnsPo::getProtocol, JsonUtil.toJson(protocol));
        }
        updateWrapper.set(UnsPo::getUpdateAt, new Date());
        update(updateWrapper);

        UnsPo afterUnsPo = baseMapper.selectById(id);
        if (afterUnsPo != null) {
            sendTempUpdateEvent(afterUnsPo);
        }
        return ResultVO.success("ok");
    }

    private void sendTempUpdateEvent(UnsPo unsPo) {
        if (unsPo == null) {
            return;
        }
        CreateTopicDto createTopicDto = UnsConverter.po2dto(unsPo);
        CreateTopicDto[] dtos = new CreateTopicDto[1];
        dtos[0] = createTopicDto;
        List<CreateTopicDto> emptyList = Collections.emptyList();
        CreateTopicDto[] emptyArray = new CreateTopicDto[0];
        Integer pathType = unsPo.getPathType();
        if (pathType == 0) {
            UpdateInstanceEvent event = new UpdateInstanceEvent(this, emptyList, dtos, emptyArray);
            EventBus.publishEvent(event);
        } else if (pathType == 1) {
            UpdateInstanceEvent event = new UpdateInstanceEvent(this, emptyList, emptyArray, dtos);
            EventBus.publishEvent(event);
        } else if (pathType == 2) {
            UpdateInstanceEvent event = new UpdateInstanceEvent(this, List.of(createTopicDto), emptyArray, emptyArray);
            EventBus.publishEvent(event);
        }
    }

    public ResultVO updateTemplate(Long id, String name, String description) {
        if (StringUtils.hasText(name)) {
            if (name.length() > 63) {
                return ResultVO.fail(I18nUtils.getMessage("uns.template.name.length"));
            }
            List<String> allTemplateNames = unsDefinitionService.allDefinitions().stream().filter(dto -> Constants.PATH_TYPE_TEMPLATE == dto.getPathType())
                    .map(CreateTopicDto::getName).collect(Collectors.toList());
            name = PathUtil.generateUniqueName(name, allTemplateNames);
        }

        UnsPo template = getById(id);
        if (null == template) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }

        LambdaUpdateWrapper<UnsPo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UnsPo::getId, id);
        updateWrapper.set(StringUtils.hasText(name), UnsPo::getName, name);
        updateWrapper.set(StringUtils.hasText(description), UnsPo::getDescription, description);
        update(updateWrapper);

        UnsPo unsPo = baseMapper.getById(id);
        if (unsPo != null) {
            List<WebhookDataDTO> webhookData = WebhookUtils.transfer(Collections.singletonList(unsPo));
            if (!webhookData.isEmpty()) {
                EventBus.publishEvent(new SysEvent(this, ServiceEnum.UNS_SERVICE, EventMetaEnum.UNS_FILED_CHANGE,
                        ActionEnum.MODIFY, webhookData));
            }
        }

        return ResultVO.success("ok");
    }

    /**
     * 修改模型描述
     *
     * @param alias
     * @param description
     * @return
     */
    public ResultVO updateDescription(String alias, String description) {
        baseMapper.updateDescByAlias(alias, description);
        UnsPo unsPo = baseMapper.getByAlias(alias);
        if (unsPo != null) {
            List<WebhookDataDTO> webhookData = WebhookUtils.transfer(Collections.singletonList(unsPo));
            if (!webhookData.isEmpty()) {
                EventBus.publishEvent(new SysEvent(this, ServiceEnum.UNS_SERVICE, EventMetaEnum.UNS_FILED_CHANGE,
                        ActionEnum.MODIFY, webhookData));
            }
        }
        return ResultVO.success("ok");
    }

    public PageResultDTO<FileVo> pageListUnsByTemplate(Long templateId, Long pageNo, Long pageSize) {
        //模板引用的模型和实例列表
        Page<UnsPo> page = new Page<>(pageNo, pageSize, true);
        LambdaQueryWrapper<UnsPo> qw = new LambdaQueryWrapper<>();
        qw.eq(UnsPo::getModelId, templateId);
        Page<UnsPo> iPage = unsMapper.selectPage(page, qw);
        List<FileVo> fileList = iPage.getRecords().stream().map(uns -> {
            FileVo fileVo = new FileVo();
            fileVo.setUnsId(uns.getId().toString());
            fileVo.setPathType(uns.getPathType());
            fileVo.setPath(uns.getPath());
            fileVo.setName(PathUtil.getName(uns.getPath()));
            return fileVo;
        }).collect(Collectors.toList());
        return PageUtil.build(iPage, fileList);
    }

    /**
     * 添加或者删除模型字段
     *
     * @param alias
     * @param fields
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public ResultVO updateFields(String alias, FieldDefine[] fields) {
        UnsPo uns = baseMapper.getByAlias(alias);
        if (uns == null || uns.getPathType() == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }
        final int pathType = uns.getPathType();
        if (fields == null) {
            fields = new FieldDefine[0];
        }
        FieldUtils.TableFieldDefine rs = null;
        if (fields.length > 0) {
            String[] err = new String[1];
            SrcJdbcType jdbcType = SrcJdbcType.getById(uns.getDataSrcId());
            rs = FieldUtils.processFieldDefines(alias, jdbcType, fields, err, true, jdbcType != null);
            if (err[0] != null) {
                return ResultVO.fail(err[0]);
            }
        } else if (pathType == Constants.PATH_TYPE_FILE || pathType == Constants.PATH_TYPE_TEMPLATE) {
            return ResultVO.fail(I18nUtils.getMessage("uns.fieldsIsEmpty"));
        }

        // 统计数字类型的字段个数，不包含系统字段
        int totalNumberField = countNumericFields(fields);
        if (pathType == Constants.PATH_TYPE_FILE) {
            fields = rs.fields;
            uns.setTableName(rs.tableName);
        }
        Date updateTime = new Date();
        // 如果修改虚拟路径，走创建模型流程
        if (pathType == Constants.PATH_TYPE_DIR && uns.getModelId() == null) {
            baseMapper.updateModelFieldsById(uns.getId(), null, fields, totalNumberField, updateTime);
            return ResultVO.successWithData(uns.getId());
        }
        Set<String> inputFields = Arrays.stream(fields).filter(i -> !i.isSystemField()).map(FieldDefine::getName)
                .collect(Collectors.toSet());
        //过滤掉_开头的系统字段
        List<FieldDefine> oldFields = Arrays.stream(uns.getFields())
                .filter(i -> !i.isSystemField()).toList();
        // 筛选出新增和删除的属性集合
        List<FieldDefine> delFields = oldFields.stream().filter(e -> !inputFields.contains(e.getName()))
                .collect(Collectors.toList());

        // 查询模型下的实例, 以便后续修改实例的表结构
        List<UnsPo> instances = pathType == Constants.PATH_TYPE_TEMPLATE ? baseMapper.listInstancesByModel(uns.getId()) :
                (pathType == Constants.PATH_TYPE_FILE ? List.of(uns) : Collections.emptyList());

        // 如果有字段删除，需要同时删除关联的计算实例
        if (!delFields.isEmpty() && !instances.isEmpty()) {
            unsCalcService.detectReferencedCalcInstance(instances, uns.getPath(), delFields, true);
        }

        // 更新模型字段到数据库
        baseMapper.updateModelFieldsById(uns.getId(), uns.getTableName(), fields, totalNumberField, updateTime);
        if (pathType == Constants.PATH_TYPE_FILE) {
            uns.setFields(fields);
            uns.setUpdateAt(updateTime);
            uns.setNumberFields(totalNumberField);
        } else if (!instances.isEmpty()) {
            uns.setFields(fields);
            for (UnsPo inst : instances) {
                inst.setUpdateAt(updateTime);
                FieldUtils.TableFieldDefine tfd = FieldUtils.processFieldDefines(inst.getAlias(), SrcJdbcType.getById(inst.getDataSrcId()), fields, new String[1], true, true);
                inst.setTableName(tfd.tableName);
                inst.setFields(tfd.fields);
                inst.setNumberFields(totalNumberField);
            }
            // 更新实例的fields字段
            this.updateBatchById(instances);
        }
        // 更新td或者pg的实例表字段(同create事件)
        sendEvent(uns, instances);
//        if (pathType == Constants.PATH_TYPE_FILE) {
//           unsQueryService.refreshLatestMsg(uns.getId());
//        }

        // webhook send
        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(instances,
                JsonUtil.jackToJson(fields));
        if (!webhookData.isEmpty()) {
//            webhookDataPusher.push(WebhookSubscribeEvent.INSTANCE_FIELD_CHANGE, webhookData, false);
            EventBus.publishEvent(new SysEvent(this, ServiceEnum.UNS_SERVICE, EventMetaEnum.UNS_FILED_CHANGE,
                    ActionEnum.MODIFY, webhookData));
        }
        return ResultVO.success("ok");
    }


    private void sendEvent(UnsPo local, List<UnsPo> instances) {
        // 发送create事件，修改pg或者td的table schema
        List<CreateTopicDto> topics = Collections.emptyList();
        if (!instances.isEmpty()) {
            topics = instances.stream().map(p -> {
                        CreateTopicDto dto = UnsConverter.po2dto(p, false);
                        // 更新时，跳过创建流程和dashboard
                        dto.setAddFlow(false);
                        dto.setAddDashBoard(false);
                        dto.setFieldsChanged(true);
                        return dto;
                    }
            ).toList();
        }
        CreateTopicDto[] folders = null, templates = null;
        CreateTopicDto localDto = UnsConverter.po2dto(local);
        localDto.setFieldsChanged(true);
        switch (local.getPathType()) {
            case Constants.PATH_TYPE_DIR:
                folders = new CreateTopicDto[]{localDto};
                break;
            case Constants.PATH_TYPE_TEMPLATE:
                templates = new CreateTopicDto[]{localDto};
                break;
        }
        UpdateInstanceEvent event = new UpdateInstanceEvent(this, topics, folders, templates);
        EventBus.publishEvent(event);
    }

    /**
     * 创建模板 已存在直接返回
     *
     * @param createTemplateVos
     * @return Map<模板别名, 模板对象>
     */
    public Map<String, UnsPo> createTemplatesIncludeExist(
            Collection<CreateTemplateVo> createTemplateVos) {
        Map<String, UnsPo> createResult = new HashMap<>(createTemplateVos.size());

        Collection<String> aliasSet = createTemplateVos.stream().map(CreateTemplateVo::getAlias)
                .collect(Collectors.toSet());

        List<UnsPo> existsTemplates = list(new QueryWrapper<UnsPo>()
                .eq("path_type", Constants.PATH_TYPE_TEMPLATE)
                .in("alias", aliasSet).select("alias"));

        existsTemplates.forEach(tmp -> createResult.put(tmp.getAlias(), tmp));

        Set<String> existsAlias = existsTemplates.stream().map(UnsPo::getAlias)
                .collect(Collectors.toSet());

        ArrayList<UnsPo> toAdds = new ArrayList<>(createTemplateVos.size());
        for (CreateTemplateVo createTemplateVo : createTemplateVos) {
            String alias = createTemplateVo.getAlias();
            if (!existsAlias.contains(alias)) {
                UnsPo uns = new UnsPo();
                uns.setId(SuposIdUtil.nextId());
                uns.setName(createTemplateVo.getName());
                uns.setAlias(alias);

                setTemplateParent(uns);

                uns.setFields(createTemplateVo.getFields());
                uns.setDescription(createTemplateVo.getDescription());
                toAdds.add(uns);
                createResult.put(alias, uns);
            }
        }
        if (!toAdds.isEmpty()) {
            this.saveBatch(toAdds);
        }
        return createResult;
    }

    public UnsPo createTemplateIncludeExist(CreateTemplateVo createTemplateVo) {
        String name = createTemplateVo.getName();
        String alias = PathUtil.generateAlias(name, 1);
        List<UnsPo> tempList = list(new LambdaQueryWrapper<UnsPo>().eq(UnsPo::getAlias, alias));
        if (!CollectionUtils.isEmpty(tempList)) {
            return tempList.get(0);
        }
        UnsPo unsPo = new UnsPo();
        unsPo.setId(SuposIdUtil.nextId());
        unsPo.setName(name);
        setTemplateParent(unsPo);
        unsPo.setDataType(0);
        unsPo.setFields(createTemplateVo.getFields());
        unsPo.setDescription(createTemplateVo.getDescription());
        unsPo.setAlias(alias);
        save(unsPo);
        return unsPo;
    }
}
