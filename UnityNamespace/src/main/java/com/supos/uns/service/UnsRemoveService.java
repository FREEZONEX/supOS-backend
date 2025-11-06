package com.supos.uns.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.dto.AlarmRuleDefine;
import com.supos.common.dto.BatchRemoveUnsDto;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.mqtt.TopicDefinition;
import com.supos.common.enums.ActionEnum;
import com.supos.common.enums.EventMetaEnum;
import com.supos.common.enums.ServiceEnum;
import com.supos.common.enums.mount.MountSubSourceType;
import com.supos.common.event.EventBus;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.SysEvent;
import com.supos.common.event.UpdateInstanceEvent;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.dao.mapper.AlarmMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.mapper.UnsMountExtendMapper;
import com.supos.uns.dao.mapper.UnsMountMapper;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.dto.WebhookDataDTO;
import com.supos.uns.service.mount.MountCoreService;
import com.supos.uns.service.mount.MountService;
import com.supos.uns.util.UnsConverter;
import com.supos.uns.util.WebhookUtils;
import com.supos.uns.vo.RemoveResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.supos.uns.service.UnsQueryService.EXTERNAL_TOPIC_CACHE;

@Slf4j
@Service
public class UnsRemoveService extends ServiceImpl<UnsMapper, UnsPo> {
    @Resource
    UnsAttachmentService unsAttachmentService;
    @Autowired
    AlarmMapper alarmMapper;
    @Autowired
    IUnsDefinitionService unsDefinitionService;
    @Autowired
    UnsAddService unsAddService;

    @Autowired
    private UnsMountExtendMapper unsMountExtendMapper;

    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    public RemoveResult removeModelOrInstance(Long unsId, boolean withFlow, boolean withDashboard, Boolean removeRefer) {
        return removeModelOrInstance(this.getById(unsId), withFlow, withDashboard, removeRefer, true, false);
    }

    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    public ResponseEntity<RemoveResult> batchRemoveResultByAliasList(BatchRemoveUnsDto batchRemoveUnsDto) {
        List<UnsPo> unsList = this.baseMapper.selectList(new LambdaQueryWrapper<UnsPo>().in(UnsPo::getAlias, batchRemoveUnsDto.getAliasList()));
        if (CollectionUtils.isEmpty(unsList)) {
            return ResponseEntity.status(400).body(new RemoveResult(0, I18nUtils.getMessage("uns.folder.or.file.not.found")));
        }
        for (UnsPo unsPo : unsList) {
            removeModelOrInstance(unsPo, batchRemoveUnsDto.getWithFlow(), batchRemoveUnsDto.getWithDashboard(), batchRemoveUnsDto.getRemoveRefer(),
                    batchRemoveUnsDto.getCheckMount() == null ? true : batchRemoveUnsDto.getCheckMount(),
                    batchRemoveUnsDto.getOnlyRemoveChild() == null ? false : batchRemoveUnsDto.getOnlyRemoveChild());
        }
        return ResponseEntity.ok(new RemoveResult(0, "ok"));
    }

    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    public RemoveResult removeInstances(Collection<String> aliases, boolean withFlow, boolean withDashboard, Boolean removeRefer) {
        QueryWrapper<UnsPo> queryWrapper = new QueryWrapper<UnsPo>().in("path_type", 2);
        queryWrapper = queryWrapper.in("alias", aliases);

        List<UnsPo> unsPos = this.list(queryWrapper);
        RemoveResult removeResult = getRemoveResult(withFlow, withDashboard, removeRefer, unsPos.stream().map(po -> UnsConverter.po2dto(po, false)).toList());
        // webhook send
//        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(unsPos);
//        if (!webhookData.isEmpty()) {
////            webhookDataPusher.push(WebhookSubscribeEvent.INSTANCE_DELETE, webhookData, false);
//        }
        return removeResult;
    }

    public RemoveResult removeModelOrInstance(UnsPo tar, boolean withFlow, boolean withDashboard, Boolean removeRefer, boolean checkMount, boolean onlyRemoveChild) {
        if (tar == null) {
            return new RemoveResult(0, "NotFound");
        }
        CreateTopicDto cur = unsDefinitionService.getDefinitionById(tar.getId());
        if (cur == null) {
            unsAddService.reSyncCache();
            cur = unsDefinitionService.getDefinitionById(tar.getId());
            if (cur == null) {
                return new RemoveResult(0, "CacheUnsNotFound");
            }
        }
        long t0 = System.currentTimeMillis();
        List<CreateTopicDto> unsPos;
        if (tar.getPathType() == 0) {//按目录删除
            if (checkMount && tar.getMountType() != null && tar.getMountType() != 0) {
                UnsMountExtendPo mountExtend = unsMountExtendMapper.selectOne(Wrappers.lambdaQuery(UnsMountExtendPo.class).eq(UnsMountExtendPo::getTargetAlias, tar.getAlias()), false);
                if (mountExtend != null && !MountSubSourceType.isALL(mountExtend.getSourceSubType()))
                return new RemoveResult(500, I18nUtils.getMessage("uns.mount.folder.operate"));
            }

            final String layRec = tar.getLayRec();
            unsAddService.reSyncCache();
            unsPos = new ArrayList<>(16 + unsDefinitionService.getTopicDefinitionMap().size() / 4);
            for (TopicDefinition def : unsDefinitionService.getTopicDefinitionMap().values()) {
                CreateTopicDto dto = def.getCreateTopicDto();
                Integer pathType = dto.getPathType();
                if (pathType == Constants.PATH_TYPE_FILE || pathType == Constants.PATH_TYPE_DIR) {
                    String curLayRec = dto.getLayRec();
                    if (curLayRec.startsWith(layRec) && curLayRec.length() > layRec.length()) {
                        unsPos.add(dto);
                    }
                }
            }
            if (!onlyRemoveChild) {
                unsPos.add(cur);
            }
        } else {//只删除单个实例
            if (checkMount && tar.getMountType() != null && tar.getMountType() != 0) {
                return new RemoveResult(500, I18nUtils.getMessage("uns.mount.folder.operate"));
            }
            unsPos = List.of(cur);
        }
        log.debug("remove unsPos: {}", unsPos.size());
        RemoveResult removeResult = getRemoveResult(withFlow, withDashboard, removeRefer, unsPos);
        // webhook send
        List<WebhookDataDTO> webhookData = WebhookUtils.transferDto(unsPos);
        if (!webhookData.isEmpty()) {
            EventBus.publishEvent(
                    new SysEvent(this, ServiceEnum.UNS_SERVICE, EventMetaEnum.UNS_FILED_CHANGE,
                            ActionEnum.DELETE, webhookData));
        }
        long t2 = System.currentTimeMillis();
        log.info("删除的总耗时: {} ms", t2 - t0);
        return removeResult;
    }

    public RemoveResult getRemoveResult(boolean withFlow, boolean withDashboard, Boolean removeRefer, List<CreateTopicDto> unsPos) {
        RemoveResult rs = new RemoveResult();
        if (CollectionUtils.isEmpty(unsPos)) {
            return rs;
        }
        List<Long> unbindIds = new ArrayList<>();
        HashMap<Long, CreateTopicDto> calcIds = new HashMap<>();
        HashSet<Long> allIds = new HashSet<>(unsPos.size());
        ArrayList<Long> alarmIds = new ArrayList<>();
        Iterator<CreateTopicDto> itr = unsPos.iterator();
        while (itr.hasNext()) {
            CreateTopicDto po = itr.next();
            allIds.add(po.getId());
            if (po.getRefers() != null) {
                calcIds.put(po.getId(), po);
            }
            if (ObjectUtil.equal(po.getDataType(), Constants.ALARM_RULE_TYPE)) {
                alarmIds.add(po.getId());
            }
        }
        List<CreateTopicDto> calcList = Collections.emptyList();
        Map<Long, Set<Long>> calcRefmap = new TreeMap<>();
        if (removeRefer == null) {
            for (CreateTopicDto po : unsPos) {
                Map<Long, Integer> referUns = po.getRefUns();
                if (!CollectionUtils.isEmpty(referUns)) {
                    for (Long id : referUns.keySet()) {
                        if (!calcIds.containsKey(id)) {
                            RemoveResult.RemoveTip tip = new RemoveResult.RemoveTip();
                            tip.setRefs(referUns.size());
                            rs.setData(tip);
                            return rs;
                        }
                    }
                }
            }
        } else if (removeRefer) {//需要删除引用文件
            calcList = new ArrayList<>(unsPos.size());
            for (CreateTopicDto po : unsPos) {

                Map<Long, Integer> referUns = po.getRefUns();
                if (!CollectionUtils.isEmpty(referUns)) {
                    for (Long id : referUns.keySet()) {
                        if (!calcIds.containsKey(id)) {
                            if (referUns.get(id) != Constants.CITING_TYPE) {//排除引用类型，引用类型无需删除，解除引用关系
                                CreateTopicDto dto = unsDefinitionService.getDefinitionById(id);
                                if (dto != null) {
                                    calcList.add(dto);
                                    addRefer(allIds, calcRefmap, dto);
                                }
                            } else {
                                unbindIds.add(id);
                            }
                        }
                    }
                }
            }
        }
        // 删除计算实例本身，对引用的实例解除引用
        for (CreateTopicDto calcPo : calcIds.values()) {
            addRefer(allIds, calcRefmap, calcPo);
        }
        List<CreateTopicDto> forUpdates = null;
        Date updateTime = new Date();
        if (!calcRefmap.isEmpty()) {

            this.removeCalcRef(calcRefmap, updateTime);

            List<UnsPo> indirectUpdates;// 因 refer 间接更新的文件
            if (calcRefmap.size() <= 1000) {
                indirectUpdates = listByIds(calcRefmap.keySet());
            } else {
                indirectUpdates = new ArrayList<>(calcRefmap.size() + unbindIds.size());
                for (List<Long> partIds : Lists.partition(new ArrayList<>(calcRefmap.keySet()), 1000)) {
                    indirectUpdates.addAll(listByIds(partIds));
                }
            }
            forUpdates = indirectUpdates.stream().map(p -> UnsConverter.po2dto(p, false)).collect(Collectors.toList());
        }
        //引用类型解除绑定关系
        if (!CollectionUtils.isEmpty(unbindIds)) {
            if (forUpdates == null) {
                forUpdates = new ArrayList<>(unbindIds.size());
            }
            InstanceField[] emptyRefers = new InstanceField[0];
            for (List<Long> segment : Lists.partition(unbindIds, 1000)) {
                LambdaUpdateWrapper<UnsPo> unbindWrapper = new LambdaUpdateWrapper<>();
                unbindWrapper.in(UnsPo::getId, segment);
                unbindWrapper.set(UnsPo::getRefers, emptyRefers);
                unbindWrapper.set(UnsPo::getUpdateAt, updateTime);
                this.update(unbindWrapper);
                for (Long id : segment) {
                    CreateTopicDto dto = unsDefinitionService.getDefinitionById(id);
                    if (dto == null) {
                        UnsPo po = getById(id);
                        if (po != null) {
                            dto = UnsConverter.po2dto(po, false);
                        }
                    }
                    if (dto != null) {
                        dto.setRefers(emptyRefers);
                        dto.setUpdateAt(updateTime);
                        forUpdates.add(dto);
                    }
                }
            }
        }
        if (!alarmIds.isEmpty()) {
            for (List<Long> alarmUnsId : Lists.partition(alarmIds, Constants.SQL_BATCH_SIZE)) {
                log.info("删除告警数据：topic = {}", alarmUnsId);
                alarmMapper.delete(new QueryWrapper<AlarmPo>().in(AlarmRuleDefine.FIELD_UNS_ID, alarmUnsId));
            }
        }
        unsAttachmentService.deleteByUns(unsPos);
        if (!CollectionUtils.isEmpty(forUpdates)) {
            long t0 = System.currentTimeMillis();
            UpdateInstanceEvent event = new UpdateInstanceEvent(this, forUpdates);
            EventBus.publishEvent(event);
            long t1 = System.currentTimeMillis();
            log.info("删除伴随的更新耗时：{} ms, 更新: {} 条", t1 - t0, forUpdates.size());
        }
        long t0 = System.currentTimeMillis();
        List<List<CreateTopicDto>> parts = Lists.partition(Stream.concat(unsPos.stream(), calcList.stream()).toList(), 1000);
        final int TOTAL = unsPos.size() + calcList.size();
        for (List<CreateTopicDto> list : parts) {
            Date delTime = new Date();
            this.removeBatchByIds(list.stream().map(CreateTopicDto::getId).toList());
            Map<Integer, List<CreateTopicDto>> groups = list.stream().collect(Collectors.groupingBy(CreateTopicDto::getPathType));
            RemoveTopicsEvent event = new RemoveTopicsEvent(this, delTime, withFlow, withDashboard,
                    groups.get(Constants.PATH_TYPE_FILE), groups.get(Constants.PATH_TYPE_TEMPLATE), groups.get(Constants.PATH_TYPE_DIR));
            EventBus.publishEvent(event);
        }
        //TODO 发送删除事件
        long t1 = System.currentTimeMillis();
        log.info("删除的 SQL 耗时: {} ms, delSize={}", t1 - t0, TOTAL);
        return rs;
    }

    private void removeCalcRef(Map<Long, Set<Long>> calcRefmap, Date updateAt) {
        this.executeBatch(calcRefmap.entrySet(), (session, entry) -> {
            UnsMapper mapper = session.getMapper(UnsMapper.class);
            Long id = entry.getKey();
            Set<Long> calcIds = entry.getValue();
            mapper.removeRefUns(id, calcIds, updateAt);
        });
    }

//    @Transactional(rollbackFor = Throwable.class, timeout = 300)
//    public RemoveResult removeInstances(Collection<String> aliases, boolean withFlow, boolean withDashboard, Boolean removeRefer) {
//        QueryWrapper<UnsPo> queryWrapper = new QueryWrapper<UnsPo>().in("path_type", 2);
//        QueryWrapper<UnsPo> removeQuery = new QueryWrapper<>();
//        removeQuery.in("alias", aliases);
//        queryWrapper = queryWrapper.in("alias", aliases);
//
//        List<UnsPo> unsPos = this.list(queryWrapper);
//        RemoveResult removeResult = getRemoveResult(withFlow, withDashboard, removeRefer, removeQuery, unsPos);
//        // webhook send
//        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(unsPos);
//        if (!webhookData.isEmpty()) {

    /// /            webhookDataPusher.push(WebhookSubscribeEvent.INSTANCE_DELETE, webhookData, false);
//        }
//        return removeResult;
//    }
    public RemoveResult detectRefers(Long id) {
        RemoveResult rs = new RemoveResult();
        UnsPo tar = this.baseMapper.getById(id);
        if (tar == null) {
            rs.setCode(400);
            rs.setMsg(I18nUtils.getMessage("uns.folder.or.file.not.found"));
            return rs;
        }

        if (tar.getPathType() == 0) {//按目录
            final String layRec = tar.getLayRec();
            for (TopicDefinition def : unsDefinitionService.getTopicDefinitionMap().values()) {
                CreateTopicDto dto = def.getCreateTopicDto();
                Integer pathType = dto.getPathType();
                if (pathType == Constants.PATH_TYPE_FILE || pathType == Constants.PATH_TYPE_DIR) {
                    String curLayRec = dto.getLayRec();
                    if (curLayRec.startsWith(layRec)) {
                        if (!CollectionUtils.isEmpty(dto.getRefUns())) {
                            RemoveResult.RemoveTip tip = new RemoveResult.RemoveTip();
                            tip.setRefs(dto.getRefUns().size());
                            rs.setData(tip);
                            return rs;
                        }
                    }
                }
            }
        }

        Map<Long, Integer> referUns = tar.getRefUns();
        if (!CollectionUtils.isEmpty(referUns)) {
            RemoveResult.RemoveTip tip = new RemoveResult.RemoveTip();
            tip.setRefs(referUns.size());
            rs.setData(tip);
            return rs;
        }
        return new RemoveResult(200, "ok");
    }

    private void addRefer(HashSet<Long> allIds, Map<Long, Set<Long>> calcRefmap, CreateTopicDto po) {
        InstanceField[] refs = po.getRefers();
        Long calcId = po.getId();
        for (InstanceField rf : refs) {
            if (rf != null) {
                Long ref = rf.getId();
                if (!allIds.contains(ref)) {
                    // 要删除的计算实例 引用的其他范围的实例，需要解除到计算实例的引用
                    calcRefmap.computeIfAbsent(ref, k -> new TreeSet<>()).add(calcId);
                }
            }
        }
    }

    public static void batchRemoveExternalTopic(Collection<CreateTopicDto[]> topics) {
        for (CreateTopicDto[] arr : topics) {
            for (CreateTopicDto t : arr) {
                EXTERNAL_TOPIC_CACHE.remove(t.getTopic());
            }
        }
    }

}
