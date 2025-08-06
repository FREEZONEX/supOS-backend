package com.supos.uns.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.*;
import com.supos.common.enums.ActionEnum;
import com.supos.common.enums.EventMetaEnum;
import com.supos.common.enums.ServiceEnum;
import com.supos.common.event.BatchCreateTableEvent;
import com.supos.common.event.EventBus;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.SysEvent;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.FieldUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.PathUtil;
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
import static com.supos.uns.service.UnsAddService.nextId;

@Service
@Slf4j
public class UnsTemplateService extends ServiceImpl<UnsMapper, UnsPo> {

    @Autowired
    UnsRemoveService unsRemoveService;
    @Autowired
    UnsConverter unsConverter;
    @Autowired
    UnsCalcService unsCalcService;
    @Autowired
    UnsMapper unsMapper;
    @Autowired
    AlarmMapper alarmMapper;
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

        String fieldError = FieldUtils.validateFields(createTemplateVo.getFields(), true);
        if (StringUtils.hasText(fieldError)){
            return ResultVO.fail(fieldError);
        }

        UnsPo unsPo = new UnsPo();
        unsPo.setId(nextId());
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
    public Map<String, String> createTemplates(List<CreateTemplateVo> createTemplateVos) {
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
            unsPo.setId(nextId());
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
        QueryWrapper<UnsPo> queryWrapper = new QueryWrapper<UnsPo>().eq("model_id", id)
                .eq("path_type", Constants.PATH_TYPE_FILE);
        List<UnsPo> unsPos = this.list(queryWrapper);
        unsPos.add(template);

        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(unsPos);
        if (!webhookData.isEmpty()) {
            EventBus.publishEvent(new SysEvent(this, ServiceEnum.UNS_SERVICE, EventMetaEnum.UNS_FILED_CHANGE,
                    ActionEnum.DELETE, webhookData));
        }
        List<CreateTopicDto> dtoList = unsPos.stream().map(p -> unsConverter.po2dto(p, false)).toList();
        return unsRemoveService.getRemoveResult(true, true, true, dtoList);
    }

    public ResultVO updateTemplate(Long id, String name, String description) {
        if (StringUtils.hasText(name)) {
            if (name.length() > 63) {
                return ResultVO.fail(I18nUtils.getMessage("uns.template.name.length"));
            }
            long count = count(
                    new LambdaQueryWrapper<UnsPo>().eq(UnsPo::getName, name)
                            .eq(UnsPo::getPathType, Constants.PATH_TYPE_TEMPLATE).ne(UnsPo::getId, id));
            if (count > 0) {
                return ResultVO.fail(I18nUtils.getMessage("uns.template.name.already.exists"));
            }
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
        String[] err = new String[1];
        FieldUtils.processFieldDefines(alias, null, fields, err, true, false);
        if (err[0] != null) {
            return ResultVO.fail(err[0]);
        }
        UnsPo uns = baseMapper.getByAlias(alias);
        if (uns == null || uns.getPathType() == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        } else if (Constants.PATH_TYPE_FILE == uns.getPathType()) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not"));
        }

        // 统计数字类型的字段个数，不包含系统字段
        int totalNumberField = countNumericFields(fields);

        // 自动添加系统字段 _ct _qos _id
//        String[] errors = new String[1];
//        FieldDefine[] autoFilledFields = processFieldDefines(uns.getPath(), uns.getDataType(), uns.getDataSrcId(), fields, errors, true);
//        if (StringUtils.hasText(errors[0])) {
//            throw new BuzException(errors[0]);
//        }
        // 如果修改虚拟路径，走创建模型流程
        if (uns.getPathType() == Constants.PATH_TYPE_DIR && uns.getModelId() == null) {
            baseMapper.updateModelFieldsById(uns.getId(), fields, totalNumberField);
            return ResultVO.successWithData(uns.getId());
        }
        Set<String> inputFields = Arrays.stream(fields).map(FieldDefine::getName)
                .collect(Collectors.toSet());
        //过滤掉_开头的系统字段
        List<FieldDefine> oldFields = Arrays.stream(uns.getFields())
                .filter(i -> !i.isSystemField()).toList();
        // 筛选出新增和删除的属性集合
        List<FieldDefine> delFields = oldFields.stream().filter(e -> !inputFields.contains(e.getName()))
                .collect(Collectors.toList());

        // 查询模型下的实例, 以便后续修改实例的表结构
        List<UnsPo> instances = baseMapper.listInstancesByModel(uns.getId());

        // 如果有字段删除，需要同时删除关联的计算实例
        List<UnsPo> delInstList = null; // 包含计算和告警实例
        List<Long> delAlarmIds = null; // 告警实例
        if (!delFields.isEmpty()) {
            delInstList = unsCalcService.detectReferencedCalcInstance(instances, uns.getPath(), delFields,
                    true);
            delAlarmIds = delInstList.stream().filter(i -> i.getDataType() == 5).map(UnsPo::getId)
                    .collect(Collectors.toList());
        }
        // 筛选出告警实例，然后删除告警历史数据
        if (delAlarmIds != null && !delAlarmIds.isEmpty()) {
            alarmMapper.deleteByUnsIds(delAlarmIds);
            log.info("---批量删除关联告警数据, unsIds为： {}", delAlarmIds);
        }

        // 更新模型字段到数据库
        baseMapper.updateModelFieldsById(uns.getId(), fields, totalNumberField);
        if (!instances.isEmpty()) {
            for (UnsPo inst : instances) {
                FieldDefine[] fsArr = inst.getFields();
                LinkedList<FieldDefine> fs = new LinkedList<>(Arrays.asList(fsArr));
                FieldDefines tmplDefines = new FieldDefines(fields);
                SrcJdbcType jdbcType = SrcJdbcType.getById(inst.getDataSrcId());
                FieldDefine ct = FieldUtils.getTimestampField(fsArr), qos = FieldUtils.getQualityField(fsArr, jdbcType.typeCode);
                fs.removeIf(fd -> !(fd == ct || fd == qos || fd.getName().startsWith(Constants.SYSTEM_FIELD_PREV)) && !tmplDefines.getFieldsMap().containsKey(fd.getName()));

                ListIterator<FieldDefine> iterator = fs.listIterator(fs.size());
                int i = fs.size();
                while (iterator.hasPrevious()) {
                    FieldDefine fd = iterator.previous();
                    if (!(fd == ct || fd == qos || fd.getName().startsWith(Constants.SYSTEM_FIELD_PREV))) {
                        iterator.next();
                        break;
                    }
                    i--;
                }
                FieldDefines defines = new FieldDefines(fsArr);
                LinkedList<FieldDefine> adds = new LinkedList<>();
                for (FieldDefine in : fields) {
                    if (!defines.getFieldsMap().containsKey(in.getName())) {
                        adds.add(in);
                    }
                }
                fs.addAll(i, adds);
                fields = fs.toArray(new FieldDefine[0]);
                FieldUtils.TableFieldDefine tfd = FieldUtils.processFieldDefines(inst.getAlias(), jdbcType, fields, new String[1], true, true);
                fields = tfd.fields;
                inst.setTableName(tfd.tableName);
                inst.setFields(fields);
                inst.setNumberFields(totalNumberField);
            }
            // 更新实例的fields字段
            this.updateBatchById(instances);
        }

        // 更新td或者pg的实例表字段(同create事件)
        sendEvent(instances, delInstList);
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

    private void sendEvent(List<UnsPo> instances, List<UnsPo> delCalcInst) {
        // 发送 delete计算实例事件
        if (delCalcInst != null && delCalcInst.size() > 0) {
            HashMap<SrcJdbcType, Map<Long, SimpleUnsInstance>> typeListMap = new HashMap<>();
            for (UnsPo po : delCalcInst) {
                SimpleUnsInstance sui = new SimpleUnsInstance(po.getId(), po.getPath(), po.getAlias(),
                        po.getTableName(), po.getDataType(), po.getParentId(), false,false, po.getFields(), po.getName());
                SrcJdbcType srcJdbcType = SrcJdbcType.getById(po.getDataSrcId());
                Map<Long, SimpleUnsInstance> calcInstances = typeListMap.computeIfAbsent(srcJdbcType, k -> new HashMap<>());
                calcInstances.put(po.getId(), sui);
            }
            for (Map.Entry<SrcJdbcType, Map<Long, SimpleUnsInstance>> entry : typeListMap.entrySet()) {
                SrcJdbcType jdbcType = entry.getKey();
                Map<Long, SimpleUnsInstance> calcInstances = entry.getValue();
                RemoveTopicsEvent removeTopicsEvent = new RemoveTopicsEvent(this, jdbcType, calcInstances,
                        false, false, null);
                EventBus.publishEvent(removeTopicsEvent);
            }
        }
        // 发送create事件，修改pg或者td的table schema
        if (!instances.isEmpty()) {
            HashMap<SrcJdbcType, ArrayList<CreateTopicDto>> jdbcFiles = new HashMap<>(2);
            instances.forEach(p -> {
                CreateTopicDto dto = unsConverter.po2dto(p);
                // 更新时，跳过创建流程和dashboard
                dto.setAddFlow(false);
                dto.setAddDashBoard(false);
                jdbcFiles.computeIfAbsent(dto.getDataSrcId(), k -> new ArrayList<>(instances.size())).add(dto);
            });
            Map tmp = jdbcFiles;
            for (Map.Entry<SrcJdbcType, ArrayList<CreateTopicDto>> entry : jdbcFiles.entrySet()) {
                tmp.replace(entry.getKey(), entry.getValue().toArray(new CreateTopicDto[0]));
            }
            Map<SrcJdbcType, CreateTopicDto[]> topics = tmp;
            BatchCreateTableEvent event = new BatchCreateTableEvent(this, false, topics);
            EventBus.publishEvent(event);
        }
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
                uns.setId(nextId());
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
        unsPo.setId(nextId());
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
