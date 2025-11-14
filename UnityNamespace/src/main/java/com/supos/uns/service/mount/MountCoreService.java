package com.supos.uns.service.mount;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supos.common.Constants;
import com.supos.common.adpater.TopicMessageConsumer;
import com.supos.common.dto.*;
import com.supos.common.dto.mount.meta.common.CommonFileMetaDto;
import com.supos.common.dto.mount.meta.common.CommonFolderMetaDto;
import com.supos.common.enums.FolderDataType;
import com.supos.common.enums.mount.MountModel;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.enums.mount.MountStatus;
import com.supos.common.enums.mount.MountSubSourceType;
import com.supos.common.exception.BuzException;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.FieldUtils;
import com.supos.common.utils.RuntimeUtil;
import com.supos.uns.bo.CreateModelInstancesArgs;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.mapper.UnsMountExtendMapper;
import com.supos.uns.dao.mapper.UnsMountMapper;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.UnsAddService;
import com.supos.uns.service.UnsQueryService;
import com.supos.uns.service.UnsRemoveService;
import com.supos.uns.service.mount.adpter.MountAdpter;
import com.supos.uns.service.mount.collector.CollectorMountAdpter;
import com.supos.uns.service.mount.kafka.KafkaMountAdpter;
import com.supos.uns.service.mount.mqtt.MqttMountAdpter;
import com.supos.uns.util.UnsConverter;
import com.supos.uns.vo.OuterStructureVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: MountCoreService
 * @date 2025/9/22 10:04
 */
@Slf4j
@Service
public class MountCoreService {

    @Autowired
    private UnsMountMapper unsMountMapper;

    @Autowired
    private UnsMountExtendMapper unsMountExtendMapper;

    @Autowired
    private UnsAddService unsAddService;

    @Autowired
    private UnsRemoveService unsRemoveService;

    @Autowired
    private IUnsDefinitionService unsDefinitionService;

    @Autowired
    private UnsMapper unsMapper;

    @Autowired
    private UnsQueryService unsQueryService;

    @Autowired
    private TopicMessageConsumer topicMessageConsumer;

    private List<MountAdpter> mountAdpters = new ArrayList<>();

    public UnsPo queryUnsByAlias(String alias) {
        return unsMapper.getByAlias(alias);
    }

    public List<UnsPo> listByAlias(Set<String> aliases) {
        return unsMapper.listByAlias(aliases);
    }

    public List<UnsPo> listByParentAlias(String parentAlias) {
        return unsMapper.selectList(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getParentAlias, parentAlias));
    }

    public <T> T getAdapter(Class<T> clazz) {
        for (MountAdpter mountAdpter : mountAdpters) {
            if (clazz.isInstance(mountAdpter)) {
                return (T)mountAdpter;
            }
        }
        return null;
    }

    public CreateTopicDto getDefinitionByAlias(String alias) {
        return unsDefinitionService.getDefinitionByAlias(alias);
    }

    private ScheduledExecutorService refreshExecutor;

    {
        refreshExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            AtomicInteger threadNum = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Mount-refresh-" + threadNum.incrementAndGet());
            }
        });
    }

    private void startTask() {
        refreshExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    removeInvalidedMountInfo();
                    handleMount();
                }catch (Throwable throwable) {
                    log.error("refreshMount error", throwable);
                }
            }
        }, 5, 30, TimeUnit.SECONDS);
    }

    @EventListener(classes = ContextRefreshedEvent.class)
    @Order(100)
    public void onStartup(ContextRefreshedEvent event) {
        if (RuntimeUtil.isLocalRuntime()) {
            return;
        }
        mountAdpters.add(new CollectorMountAdpter(this));
        mountAdpters.add(new MqttMountAdpter(this));
        mountAdpters.add(new KafkaMountAdpter(this));
        // 开启定时任务
        startTask();
    }

    private void closeSubscribeBySourceAlias(String sourceName, Integer sourceType) {
        if (sourceType == MountSourceType.MQTT.getTypeValue()){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("connectName", sourceName);
            MqttMountAdpter adapter = getAdapter(MqttMountAdpter.class);
            if (adapter != null) {
                adapter.getMqttAdpter().closeMqttClient(jsonObject);
            }
        } else if (sourceType == MountSourceType.KAFKA.getTypeValue()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("connectName", sourceName);
            KafkaMountAdpter adapter = getAdapter(KafkaMountAdpter.class);
            if (adapter != null) {
                adapter.getKafkaAdapter().disconnect(jsonObject);
            }
        }
    }

    /**
     * 刷新挂载信息，去除无效挂载
     */
    private void removeInvalidedMountInfo() {
        // 移除无效主挂载
        List<UnsMountPo> mountPos = unsMountMapper.selectList(Wrappers.lambdaQuery(UnsMountPo.class));
        if (CollectionUtil.isEmpty(mountPos)) {
            return;
        }
        Set<String> mainMountTargetAliases = mountPos.stream().map(UnsMountPo::getTargetAlias).collect(Collectors.toSet());
        List<UnsPo> mainMountTargets = unsMapper.listByAlias(mainMountTargetAliases);
        Set<String> existMainMountTargetAliases = mainMountTargets.stream().map(UnsPo::getAlias).collect(Collectors.toSet());

        for (UnsMountPo mount : mountPos) {
            if (!existMainMountTargetAliases.contains(mount.getTargetAlias())) {
                // 删除无效的挂载
                unsMountMapper.delete(Wrappers.lambdaQuery(UnsMountPo.class).eq(UnsMountPo::getTargetAlias, mount.getTargetAlias()));
                if (mount.getSourceType() != null) {
                    closeSubscribeBySourceAlias(mount.getSourceAlias(), mount.getSourceType());
                }
                //unsMountVxbaseCoreService.invalidate(mount.getSourceAlias());
            }
        }

        // 移除无效辅挂载
        List<UnsMountExtendPo> mountExtendPos = unsMountExtendMapper.selectList(Wrappers.lambdaQuery(UnsMountExtendPo.class));
        if (CollectionUtil.isEmpty(mountExtendPos)) {
            return;
        }
        Set<String> subMountTargetAliases = mountExtendPos.stream().map(UnsMountExtendPo::getTargetAlias).collect(Collectors.toSet());
        List<UnsPo> subMountTargets = unsMapper.listByAlias(subMountTargetAliases);
        Set<String> existSubMountTargetAliases = subMountTargets.stream().map(UnsPo::getAlias).collect(Collectors.toSet());

        for (UnsMountExtendPo mount : mountExtendPos) {
            if (!existSubMountTargetAliases.contains(mount.getTargetAlias())) {
                // 删除无效的挂载
                unsMountExtendMapper.delete(Wrappers.lambdaQuery(UnsMountExtendPo.class).eq(UnsMountExtendPo::getTargetAlias, mount.getTargetAlias()));
                //unsMountVxbaseCoreService.invalidate(mount.getSourceAlias());
            }
        }
    }

    private void handleMount() {
        for (MountAdpter mountAdpter : mountAdpters) {
            mountAdpter.handleMount();
        }
    }

    /**
     * 创建文件夹
     * @param parentAlias
     * @param folders
     */
    public void createFolder(String parentAlias, List<CommonFolderMetaDto> folders) {
        List<CreateTopicDto> topics = new ArrayList<>(folders.size());
        AtomicInteger index = new AtomicInteger();
        for (CommonFolderMetaDto folder : folders) {
            CreateTopicDto topic = new CreateTopicDto();
            topic.setName(folder.getName());
            topic.setAlias(folder.getCode());
            topic.setDisplayName(folder.getDisplayName());
            topic.setParentAlias(parentAlias);
            topic.setPathType(Constants.PATH_TYPE_DIR);
            topic.setMountType(folder.getMountType());
            topic.setMountSource(folder.getMountSource());
            topic.setIndex(index.getAndIncrement());
            topics.add(topic);
        }

        CreateModelInstancesArgs args = new CreateModelInstancesArgs();
        args.setTopics(topics);
        args.setFromImport(false);
        args.setThrowModelExistsErr(false);
        log.info("args: {}", JSONObject.toJSONString(args));
        Map<String, String> rs = unsAddService.createModelAndInstancesInner(args);

        if (MapUtils.isNotEmpty(rs)) {
            throw new RuntimeException("create device folder error:" + JSONObject.toJSONString(rs));
        }
    }

    /**
     * 清理文件夹（不会删除folder对应的文件夹，仅删除folder的下级文件夹和文件）
     * @param folder
     */
    public void clearFolder(UnsPo folder) {
        log.info("delete mount folder {} children", folder.getAlias());
        // 删除文件夹
        unsRemoveService.removeModelOrInstance(folder, false, false, false, false, true);
    }

    /**
     * 删除文件夹
     * @param folder
     */
    public void removeFolder(UnsPo folder) {
        log.info("delete mount folder {}", folder.getAlias());
        // 删除文件夹
        unsRemoveService.removeModelOrInstance(folder, false, false, false, false, false);
    }

    public void clearFolderMount(UnsPo folder) {
        log.info("clear mount folder target：{}", folder.getAlias());
        CreateTopicDto dto = UnsConverter.po2dto(folder);
        dto.setMountType(0);
        dto.setMountSource(null);
        unsAddService.createModelInstance(dto);
    }

    /**
     *
     * 创建挂载文件
     * @param mountSourceType 挂载类型
     * @param parentAlias 上级文件夹（在哪个文件夹下创建文件）
     * @param mountPo 挂载信息
     * @param files 文件信息
     */
    public void saveFile(MountSourceType mountSourceType, String parentAlias, UnsMountPo mountPo, List<CommonFileMetaDto> files) {
        List<CommonFileMetaDto> addFiles = new LinkedList<>();
        Map<CommonFileMetaDto, CreateTopicDto> updateFiles = new HashMap<>();

        for (CommonFileMetaDto fileMeta : files) {
            CreateTopicDto existTagFile = unsDefinitionService.getDefinitionByAlias(fileMeta.getAlias());
            if (existTagFile == null) {
                addFiles.add(fileMeta);
            } else {
                if (existTagFile.getMountType() != null && mountSourceType.getTypeValue() == existTagFile.getMountType()) {
                    updateFiles.put(fileMeta, existTagFile);
                } else {
                    log.warn("found exist tag {} is not mount by {}, skip.", fileMeta.getAlias(), mountSourceType.getType());
                    continue;
                }
            }
        }

        if (CollectionUtil.isNotEmpty(addFiles)) {
            addFile(mountSourceType, parentAlias, mountPo, addFiles);
        }

        if (MapUtils.isNotEmpty(updateFiles)) {
            updateFile(mountSourceType, parentAlias, mountPo, updateFiles);
        }
    }

    private void addFile(MountSourceType mountSourceType, String parentAlias, UnsMountPo mountPo, List<CommonFileMetaDto> addFiles) {
        AtomicInteger index = new AtomicInteger();
        List<CreateTopicDto> topics = new ArrayList<>(addFiles.size());
        Map<String, String> tagIndexMap = new HashMap<>(addFiles.size());
        int flag = mountPo.getWithFlags() != null ? mountPo.getWithFlags() : 0;
        int dataType = mountPo.getDataType() != null ? mountPo.getDataType() : Constants.TIME_SEQUENCE_TYPE;
        for (CommonFileMetaDto fileMeta : addFiles) {
            CreateTopicDto topicDto = new CreateTopicDto();
            topicDto.setBatch(0);
            topicDto.setIndex(index.getAndIncrement());
            topicDto.setName(fileMeta.getName());
            topicDto.setDisplayName(fileMeta.getDisplayName());
            topicDto.setParentAlias(parentAlias);
            topicDto.setAlias(fileMeta.getAlias());
            topicDto.setPathType(Constants.PATH_TYPE_FILE);
            topicDto.setDataType(dataType);
            topicDto.setDescription(fileMeta.getDescription());
            topicDto.setAddDashBoard(MountFlag.withDashBoard(flag));
            topicDto.setAddFlow(false);
            // 设置metrics\时序类型文件
            topicDto.setParentDataType(FolderDataType.METRIC.getTypeIndex());
            topicDto.setDataType(Constants.TIME_SEQUENCE_TYPE);

            topicDto.setMountType(mountSourceType.getTypeValue());
            topicDto.setMountSource(mountPo.getSourceAlias());
            if (fileMeta.getSave2db() != null) {
                topicDto.setSave2db(fileMeta.getSave2db());
            } else {
                topicDto.setSave2db(MountFlag.withSave2db(flag));
            }

            topicDto.setFields(fileMeta.getFields());
            topicDto.setExtendFieldUsed(FieldUtils.getExtendFieldUsed());

            topics.add(topicDto);
            tagIndexMap.put(String.valueOf(topicDto.getIndex()), fileMeta.getAlias());
        }

        if (CollectionUtil.isNotEmpty(topics)) {
            log.info("add mount file size:{} for target:{}", topics.size(), parentAlias);
            CreateModelInstancesArgs args = new CreateModelInstancesArgs();
            args.setTopics(topics);
            args.setFromImport(false);
            args.setThrowModelExistsErr(false);
            Map<String, String> rs = unsAddService.createModelAndInstancesInner(args);
            if (MapUtils.isNotEmpty(rs)) {
                JSONArray errors = new JSONArray(rs.size());
                rs.entrySet().forEach(e -> {
                    String tagIndex = e.getKey();
                    String[] tagArr = tagIndex.split("-");
                    String tagName = tagIndexMap.get(tagArr[1]);
                    JSONObject error = new JSONObject();
                    error.put("tag", tagName);
                    error.put("error", e.getValue());
                    errors.add(error);
                });
                throw new BuzException(JSONObject.toJSONString(errors));
            }
        }
    }

    private void updateFile(MountSourceType mountSourceType, String parentAlias, UnsMountPo mountPo, Map<CommonFileMetaDto, CreateTopicDto> updateFiles) {
        int flag = mountPo.getWithFlags() != null ? mountPo.getWithFlags() : 0;
        int dataType = mountPo.getDataType() != null ? mountPo.getDataType() : Constants.TIME_SEQUENCE_TYPE;
        for (Map.Entry<CommonFileMetaDto, CreateTopicDto> e : updateFiles.entrySet()) {
            // 更新文件
            CommonFileMetaDto updateFile = e.getKey();
            if (updateFile.getSave2db() == null) {
                updateFile.setSave2db(MountFlag.withSave2db(flag));
            }
            if (!isTagFileChange(e.getValue(), updateFile)) {
                continue;
            }

            UpdateUnsDto dto = new UpdateUnsDto();
            dto.setName(updateFile.getName());
            dto.setAlias(e.getValue().getAlias());
            dto.setDisplayName(updateFile.getDisplayName());
            dto.setDescription(updateFile.getDescription());
            dto.setParentAlias(parentAlias);
            dto.setPathType(Constants.PATH_TYPE_FILE);
            dto.setAddDashBoard(MountFlag.withDashBoard(flag));
            dto.setAddFlow(false);
            //dto.setMountType(mountSourceType.getTypeValue());
            //dto.setMountSource(fileMeta.getOriginalAlias());
            dto.setSave2db(updateFile.getSave2db());
            dto.setFields(updateFile.getFields());
            //dto.setExtendFieldUsed(FieldUtils.getExtendFieldUsed());
            unsAddService.updateModelInstance(dto);

            // 修改属性

        }
    }

    /**
     * 判断文件是否进行了修改
     * @param oldFile
     * @param newFile
     * @return
     */
    private boolean isTagFileChange(CreateTopicDto oldFile, CommonFileMetaDto newFile) {
        // 判断描述、显示名是否修改了
        if (!StringUtils.equals(oldFile.getDescription(), newFile.getDescription())
                || !StringUtils.equals(oldFile.getDisplayName(), newFile.getDisplayName())) {
            return true;
        }

        FieldDefine[] oldFields = oldFile.getFields();
        FieldDefine[] newFields = newFile.getFields();
        Map<String, FieldDefine> oldFieldMap = Arrays.stream(oldFields).filter(field -> !field.isSystemField()).collect(Collectors.toMap(FieldDefine::getName, Function.identity(), (k1, k2) -> k2));
        for (FieldDefine newField : newFields) {
            FieldDefine oldField = oldFieldMap.get(newField.getName());
            if (oldField == null) {
                if (!StringUtils.equals(newField.getUnit(), oldField.getUnit()))
                return true;
            }
        }

        Boolean newSave2db = newFile.getSave2db();
        Boolean oldSave2db = oldFile.getSave2db();
        if ((newSave2db && !oldSave2db) || (!newSave2db && oldSave2db)) {
            return true;
        }

        return false;
    }

    public void updateFile(UpdateUnsDto dto) {
        unsAddService.updateModelInstance(dto);
    }

    public void deleteFile(List<String> removeAlias) {
        BatchRemoveUnsDto batchRemoveUnsDto = new BatchRemoveUnsDto();
        batchRemoveUnsDto.setAliasList(removeAlias);
        batchRemoveUnsDto.setWithFlow(false);
        batchRemoveUnsDto.setWithDashboard(false);
        batchRemoveUnsDto.setRemoveRefer(false);
        batchRemoveUnsDto.setCheckMount(false);
        batchRemoveUnsDto.setOnlyRemoveChild(false);
        unsRemoveService.batchRemoveResultByAliasList(batchRemoveUnsDto);
    }

    /**
     * 查询采集器主挂载信息
     * @param mountModel
     * @param sourceAlias
     * @param mountStatus
     * @return
     */
    public List<UnsMountPo> queryMountInfo(MountModel mountModel, String sourceAlias, Integer mountStatus) {
        LambdaQueryWrapper<UnsMountPo> queryWrapper = Wrappers.lambdaQuery(UnsMountPo.class);
        if (mountModel != null) {
            queryWrapper.eq(UnsMountPo::getMountModel, mountModel.getType());
        }
        if (StringUtils.isNotBlank(sourceAlias)) {
            queryWrapper.eq(UnsMountPo::getSourceAlias, sourceAlias);
        }
        if (mountStatus != null) {
            queryWrapper.eq(UnsMountPo::getMountStatus, mountStatus);
        }
        return unsMountMapper.selectList(queryWrapper);
    }

    /**
     * 查询辅挂载信息
     * @return
     */
    public List<UnsMountExtendPo> queryMountExtendInfo(MountSubSourceType mountSubSourceType, String firstSourceAlias, String mountSeq) {
        LambdaQueryWrapper<UnsMountExtendPo> queryWrapper = Wrappers.lambdaQuery(UnsMountExtendPo.class);
        if (mountSubSourceType != null) {
            queryWrapper.eq(UnsMountExtendPo::getSourceSubType, mountSubSourceType.getType());
        }
        if (StringUtils.isNotBlank(firstSourceAlias)) {
            queryWrapper.eq(UnsMountExtendPo::getFirstSourceAlias, firstSourceAlias);
        }
        if (StringUtils.isNotBlank(mountSeq)) {
            queryWrapper.eq(UnsMountExtendPo::getMountSeq, mountSeq);
        }
        return unsMountExtendMapper.selectList(queryWrapper);
    }

    public void saveMountInfo(UnsMountPo unsMountPo) {
        unsMountMapper.insert(unsMountPo);
    }

    /**
     * 修改采集器挂载信息
     * @param unsMountPo
     */
    public void updateMountInfo(UnsMountPo unsMountPo) {
        unsMountMapper.updateById(unsMountPo);
    }

    public void updateMountStatus(Long id, MountStatus mountStatus) {
        unsMountMapper.update(Wrappers.lambdaUpdate(UnsMountPo.class)
                .set(UnsMountPo::getStatus, mountStatus.getStatus()).eq(UnsMountPo::getId, id));
    }
    public void saveMountExtend(UnsMountExtendPo mountExtendInfo) {
        unsMountExtendMapper.insert(mountExtendInfo);
    }

    public void updateMountExtend(UnsMountExtendPo mountExtendInfo) {
        unsMountExtendMapper.updateById(mountExtendInfo);
    }

    /**
     * 保存采集器辅挂载信息
     * @param mountExtendInfos
     */
    public void saveMountExtend(List<UnsMountExtendPo> mountExtendInfos) {
        unsMountExtendMapper.insert(mountExtendInfos);
    }



    /**
     * 删除挂载信息
     * @param mountSeq
     */
    public void deleteMountBySeq(String mountSeq) {
        unsMountMapper.delete(Wrappers.lambdaQuery(UnsMountPo.class).eq(UnsMountPo::getMountSeq, mountSeq));
        unsMountExtendMapper.delete(Wrappers.lambdaQuery(UnsMountExtendPo.class).eq(UnsMountExtendPo::getMountSeq, mountSeq));
    }

    public void deleteMountExtend(String mountSeq, String firstSourceAlias, String secondSourceAlias) {
        unsMountExtendMapper.delete(Wrappers.lambdaQuery(UnsMountExtendPo.class)
                .eq(UnsMountExtendPo::getMountSeq, mountSeq)
                .eq(UnsMountExtendPo::getFirstSourceAlias, firstSourceAlias)
                .eq(UnsMountExtendPo::getSecondSourceAlias, secondSourceAlias));
    }

    /**
     * 解析消息
     * @param payload
     * @return
     */
    public List<OuterStructureVo> parserTopicPayload(String payload) {
        if (StringUtils.isBlank(payload)) {
            return null;
        }

        JsonResult<List<OuterStructureVo>> result = unsQueryService.parseJson2uns(payload);
        if (result.getCode() != 0) {
            // 解析失败
            log.error("解析消息失败: {}", payload);
            return Collections.emptyList();
        }

        return result.getData();
    }

    /**
     * 保存消息
     * @param mountSourceType
     * @param topic
     * @param payload
     */
    public void saveTopicPayloadToUns(MountSourceType mountSourceType, String connectName, String topic, String payload) {
        if (StringUtils.isBlank(payload)) {
            return;
        }

        String alias = MountUtils.alias(mountSourceType, connectName, topic);
        CreateTopicDto file = unsDefinitionService.getDefinitionByAlias(alias);
        if (file == null) {
            return;
        }
        /*if (!Constants.withSave2db(file.getFlags())) {
            return;
        }*/

        List<OuterStructureVo> outerStructureVos = parserTopicPayload(payload);
        if (outerStructureVos == null) {
            return;
        }

        Pair<FieldDefine[], Map<String, Object>> fieldValues = MountUtils.topic2FileFieldsWithValue(topic, payload, outerStructureVos);
        if (fieldValues.getLeft() == null || fieldValues.getLeft().length == 0) {
            return;
        }

        // 封装值
        Map<String, Object> valueMap = fieldValues.getRight();
        JSONObject value = new JSONObject();
        file.getFieldDefines().getFieldsMap().forEach((k, v) -> {
            Object fieldValue = valueMap.get(k);
            if (fieldValue != null) {
                value.put(k, fieldValue);
            }
        });
        topicMessageConsumer.onMessageByAlias(alias, value.toJSONString());
    }


}
