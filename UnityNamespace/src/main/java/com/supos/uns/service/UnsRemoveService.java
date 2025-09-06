package com.supos.uns.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.*;
import com.supos.common.dto.mqtt.TopicDefinition;
import com.supos.common.enums.ActionEnum;
import com.supos.common.enums.EventMetaEnum;
import com.supos.common.enums.ServiceEnum;
import com.supos.common.event.EventBus;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.SysEvent;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.dao.mapper.AlarmMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.dto.WebhookDataDTO;
import com.supos.uns.util.WebhookUtils;
import com.supos.uns.vo.RemoveResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

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

    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    public RemoveResult removeModelOrInstance(Long unsId, boolean withFlow, boolean withDashboard, Boolean removeRefer) {
        return removeModelOrInstance(this.getById(unsId), withFlow, withDashboard, removeRefer);
    }

    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    public ResponseEntity<RemoveResult> batchRemoveResultByAliasList(BatchRemoveUnsDto batchRemoveUnsDto) {
        List<UnsPo> unsList = this.baseMapper.selectList(new LambdaQueryWrapper<UnsPo>().in(UnsPo::getAlias, batchRemoveUnsDto.getAliasList()));
        if (CollectionUtils.isEmpty(unsList)) {
            return ResponseEntity.status(400).body(new RemoveResult(0, I18nUtils.getMessage("uns.folder.or.file.not.found")));
        }
        for (UnsPo unsPo : unsList) {
            removeModelOrInstance(unsPo, batchRemoveUnsDto.getWithFlow(), batchRemoveUnsDto.getWithDashboard(), batchRemoveUnsDto.getRemoveRefer());
        }
        return ResponseEntity.ok(new RemoveResult(0, "ok"));
    }

    public RemoveResult removeModelOrInstance(UnsPo tar, boolean withFlow, boolean withDashboard, Boolean removeRefer) {
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
            final String layRec = tar.getLayRec();
            unsAddService.reSyncCache();
            unsPos = new ArrayList<>(16 + unsDefinitionService.getTopicDefinitionMap().size() / 4);
            for (TopicDefinition def : unsDefinitionService.getTopicDefinitionMap().values()) {
                CreateTopicDto dto = def.getCreateTopicDto();
                Integer pathType = dto.getPathType();
                if (pathType == Constants.PATH_TYPE_FILE || pathType == Constants.PATH_TYPE_DIR) {
                    String curLayRec = dto.getLayRec();
                    if (curLayRec.startsWith(layRec)) {
                        unsPos.add(dto);
                    }
                }
            }
            unsPos.add(cur);
        } else {//只删除单个实例
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
        HashMap<SrcJdbcType, TopicBaseInfoList> typeListMap = new HashMap<>();
        HashMap<Long, CreateTopicDto> calcIds = new HashMap<>();
        HashSet<Long> allIds = new HashSet<>(unsPos.size());
        ArrayList<Long> alarmIds = new ArrayList<>();
        Iterator<CreateTopicDto> itr = unsPos.iterator();
        Map<SrcJdbcType, LinkedList<String>> modelTopics = new HashMap<>();
        while (itr.hasNext()) {
            CreateTopicDto po = itr.next();
            if (po.getPathType() == 1) {
                SrcJdbcType jdbcType = po.getDataSrcId();
                String topicPath = po.getPath();
                modelTopics.computeIfAbsent(jdbcType, k -> new LinkedList<>()).add(topicPath);
            }
            addPo4Remove(typeListMap, po);
            allIds.add(po.getId());
            if (po.getRefers() != null) {
                calcIds.put(po.getId(), po);
            }
            if (ObjectUtil.equal(po.getDataType(), Constants.ALARM_RULE_TYPE)) {
                alarmIds.add(po.getId());
            }
        }
        List<Long> incQueryCalcTopics = Collections.emptyList();
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
            incQueryCalcTopics = new ArrayList<>(unsPos.size());
            for (CreateTopicDto po : unsPos) {

                Map<Long, Integer> referUns = po.getRefUns();
                if (!CollectionUtils.isEmpty(referUns)) {
                    for (Long id : referUns.keySet()) {
                        if (!calcIds.containsKey(id)) {
                            if (referUns.get(id) != Constants.CITING_TYPE) {//排除引用类型，引用类型无需删除，解除引用关系
                                incQueryCalcTopics.add(id);
                            } else {
                                unbindIds.add(id);
                            }
                        }
                    }
                }
            }
            if (!incQueryCalcTopics.isEmpty()) {
                List<Pair<Long, Long>> unRefList = new ArrayList<>();
                for (Long pId : incQueryCalcTopics) {
                    CreateTopicDto dto = unsDefinitionService.getDefinitionById(pId);
                    if (dto != null) {
                        addPo4Remove(typeListMap, dto);
                        addRefer(allIds, unRefList, dto);
                    }
                }
                if (!unRefList.isEmpty()) {
                    removeCalcRef(unRefList);
                }
            }
        }
        // 删除计算实例本身，对引用的实例解除引用
        List<Pair<Long, Long>> unRefList = new ArrayList<>();
        for (CreateTopicDto calcPo : calcIds.values()) {
            addRefer(allIds, unRefList, calcPo);
        }
        if (!unRefList.isEmpty()) {
            removeCalcRef(unRefList);
        }
        if (!typeListMap.isEmpty()) {
            long t0 = System.currentTimeMillis();
            for (Map.Entry<SrcJdbcType, TopicBaseInfoList> entry : typeListMap.entrySet()) {
                SrcJdbcType srcJdbcType = entry.getKey();
                TopicBaseInfoList v = entry.getValue();
                LinkedList<String> mps = modelTopics.get(srcJdbcType);
                RemoveTopicsEvent event = new RemoveTopicsEvent(this, srcJdbcType, v.topics, withFlow, withDashboard, mps);
                EventBus.publishEvent(event);
            }
            long t1 = System.currentTimeMillis();
            log.info("删除File事件耗时: {} ms", t1 - t0);
        }
        if (!modelTopics.isEmpty()) {
            long t0 = System.currentTimeMillis();
            for (Map.Entry<SrcJdbcType, LinkedList<String>> entry : modelTopics.entrySet()) {
                SrcJdbcType srcJdbcType = entry.getKey();
                LinkedList<String> mps = entry.getValue();
                RemoveTopicsEvent event = new RemoveTopicsEvent(this, srcJdbcType, Collections.emptyMap(), withFlow, withDashboard, mps);
                EventBus.publishEvent(event);
            }
            long t1 = System.currentTimeMillis();
            log.info("删除Model事件耗时: {} ms", t1 - t0);
        }
        if (!alarmIds.isEmpty()) {
            for (List<Long> alarmUnsId : Lists.partition(alarmIds, Constants.SQL_BATCH_SIZE)) {
                log.info("删除告警数据：topic = {}", alarmUnsId);
                alarmMapper.delete(new QueryWrapper<AlarmPo>().in(AlarmRuleDefine.FIELD_UNS_ID, alarmUnsId));
            }
        }
        unsAttachmentService.deleteByUns(unsPos);
        //引用类型解除绑定关系
        if (!CollectionUtils.isEmpty(unbindIds)) {
            LambdaUpdateWrapper<UnsPo> unbindWrapper = new LambdaUpdateWrapper<>();
            unbindWrapper.in(UnsPo::getId, unbindIds);
            unbindWrapper.set(UnsPo::getRefers, null);
            this.update(unbindWrapper);
        }

        long t0 = System.currentTimeMillis();
        for (List<Long> segment : Lists.partition(unsPos.stream().map(CreateTopicDto::getId).toList(), 1000)) {
            this.removeBatchByIds(segment);
        }
        if (!incQueryCalcTopics.isEmpty()) {
            for (List<Long> segment : Lists.partition(incQueryCalcTopics, 1000)) {
                this.removeBatchByIds(segment);
            }
        }
        long t1 = System.currentTimeMillis();
        log.info("删除的 SQL 耗时: {} ms", t1 - t0);
        return rs;
    }

    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    public RemoveResult removeInstances(Collection<String> aliases, boolean withFlow, boolean withDashboard, Boolean removeRefer) {
        QueryWrapper<UnsPo> queryWrapper = new QueryWrapper<UnsPo>().in("path_type", 2);
        QueryWrapper<UnsPo> removeQuery = new QueryWrapper<>();
        removeQuery.in("alias", aliases);
        queryWrapper = queryWrapper.in("alias", aliases);

        List<UnsPo> unsPos = this.list(queryWrapper);
        RemoveResult removeResult = getRemoveResult(withFlow, withDashboard, removeRefer, removeQuery, unsPos);
        // webhook send
        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(unsPos);
        if (!webhookData.isEmpty()) {
//            webhookDataPusher.push(WebhookSubscribeEvent.INSTANCE_DELETE, webhookData, false);
        }
        return removeResult;
    }

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

    public RemoveResult getRemoveResult(boolean withFlow, boolean withDashboard, Boolean removeRefer, QueryWrapper<UnsPo> removeQuery, List<UnsPo> unsPos) {
        RemoveResult rs = new RemoveResult();
        if (!CollectionUtils.isEmpty(unsPos)) {
            HashMap<SrcJdbcType, TopicBaseInfoList> typeListMap = new HashMap<>();
            HashMap<Long, UnsPo> calcIds = new HashMap<>();
            HashSet<Long> allIds = new HashSet<>(unsPos.size());
            ArrayList<Long> alarmIds = new ArrayList<>();
            Iterator<UnsPo> itr = unsPos.iterator();
            Map<SrcJdbcType, LinkedList<String>> modelTopics = new HashMap<>();
            List<String> aliases = new ArrayList<>(unsPos.size());
            while (itr.hasNext()) {
                UnsPo po = itr.next();
                if (po.getPathType() == 1) {
                    SrcJdbcType jdbcType = SrcJdbcType.getById(po.getDataSrcId());
                    String topicPath = po.getPath();
                    modelTopics.computeIfAbsent(jdbcType, k -> new LinkedList<>()).add(topicPath);
                    itr.remove();
                    continue;
                }
                addPo4Remove(typeListMap, po);
                allIds.add(po.getId());
                aliases.add(po.getAlias());
                if (po.getRefers() != null) {
                    calcIds.put(po.getId(), po);
                }
                if (ObjectUtil.equal(po.getDataType(), Constants.ALARM_RULE_TYPE)) {
                    alarmIds.add(po.getId());
                }
            }
            if (removeRefer == null) {
                for (UnsPo po : unsPos) {
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
            } else if (removeRefer) {
                List<Long> incQueryCalcTopics = new ArrayList<>(unsPos.size());
                for (UnsPo po : unsPos) {

                    Map<Long, Integer> referUns = po.getRefUns();
                    if (!CollectionUtils.isEmpty(referUns)) {
                        for (Long id : referUns.keySet()) {
                            if (!calcIds.containsKey(id)) {
                                incQueryCalcTopics.add(id);
                            }
                        }
                    }
                }
                if (!incQueryCalcTopics.isEmpty()) {
                    List<UnsPo> list = this.listByIds(incQueryCalcTopics);
                    List<Pair<Long, Long>> unRefList = new ArrayList<>();
                    for (UnsPo po : list) {
                        addPo4Remove(typeListMap, po);
                        addRefer(allIds, unRefList, po);
                    }
                    if (!unRefList.isEmpty()) {
                        removeCalcRef(unRefList);
                    }
                    removeQuery.or(p -> p.in("id", incQueryCalcTopics));
                }
            }
            // 删除计算实例本身，对引用的实例解除引用
            List<Pair<Long, Long>> unRefList = new ArrayList<>();
            for (UnsPo calcPo : calcIds.values()) {
                addRefer(allIds, unRefList, calcPo);
            }
            if (!unRefList.isEmpty()) {
                removeCalcRef(unRefList);
            }
            if (!typeListMap.isEmpty()) {
                long t0 = System.currentTimeMillis();
                for (Map.Entry<SrcJdbcType, TopicBaseInfoList> entry : typeListMap.entrySet()) {
                    SrcJdbcType srcJdbcType = entry.getKey();
                    TopicBaseInfoList v = entry.getValue();
                    LinkedList<String> mps = modelTopics.get(srcJdbcType);
                    RemoveTopicsEvent event = new RemoveTopicsEvent(this, srcJdbcType, v.topics, withFlow, withDashboard, mps);
                    EventBus.publishEvent(event);
                }
                long t1 = System.currentTimeMillis();
                log.info("删除File事件耗时: {} ms", t1 - t0);
            }
            if (!modelTopics.isEmpty()) {
                long t0 = System.currentTimeMillis();
                for (Map.Entry<SrcJdbcType, LinkedList<String>> entry : modelTopics.entrySet()) {
                    SrcJdbcType srcJdbcType = entry.getKey();
                    LinkedList<String> mps = entry.getValue();
                    RemoveTopicsEvent event = new RemoveTopicsEvent(this, srcJdbcType, Collections.emptyMap(), withFlow, withDashboard, mps);
                    EventBus.publishEvent(event);
                }
                long t1 = System.currentTimeMillis();
                log.info("删除Model事件耗时: {} ms", t1 - t0);
            }
            if (!alarmIds.isEmpty()) {
                for (List<Long> alarmUnsId : Lists.partition(alarmIds, Constants.SQL_BATCH_SIZE)) {
                    log.info("删除告警数据：topic = {}", alarmUnsId);
                    alarmMapper.delete(new QueryWrapper<AlarmPo>().in(AlarmRuleDefine.FIELD_UNS_ID, alarmUnsId));
                }
            }
            unsAttachmentService.delete(aliases);
        }
        long t0 = System.currentTimeMillis();
        this.remove(removeQuery);
        long t1 = System.currentTimeMillis();
        log.info("删除的 SQL 耗时: {} ms", t1 - t0);
        return rs;
    }

    private void addRefer(HashSet<Long> allIds, List<Pair<Long, Long>> unRefList, CreateTopicDto po) {
        InstanceField[] refs = po.getRefers();
        Long calcId = po.getId();
        for (InstanceField rf : refs) {
            if (rf != null) {
                Long ref = rf.getId();
                if (!allIds.contains(ref)) {
                    // 要删除的计算实例 引用的其他范围的实例，需要解除到计算实例的引用
                    unRefList.add(Pair.of(ref, calcId));
                }
            }
        }
    }

    private void addRefer(HashSet<Long> allIds, List<Pair<Long, Long>> unRefList, UnsPo po) {
        InstanceField[] refs = po.getRefers();
        Long calcId = po.getId();
        for (InstanceField rf : refs) {
            if (rf != null) {
                Long ref = rf.getId();
                if (!allIds.contains(ref)) {
                    // 要删除的计算实例 引用的其他范围的实例，需要解除到计算实例的引用
                    unRefList.add(Pair.of(ref, calcId));
                }
            }
        }
    }

    private void removeCalcRef(List<Pair<Long, Long>> unRefList) {
        this.executeBatch(unRefList, (session, pair) -> {
            UnsMapper mapper = session.getMapper(UnsMapper.class);
            Long id = pair.getLeft();
            Long calcId = pair.getRight();
            mapper.removeRefUns(id, Collections.singletonList(calcId));
        });
    }

    private void addPo4Remove(HashMap<SrcJdbcType, TopicBaseInfoList> typeListMap, UnsPo po) {
        SrcJdbcType jdbcType = SrcJdbcType.getById(po.getDataSrcId());
        TopicBaseInfoList list = typeListMap.computeIfAbsent(jdbcType, k -> new TopicBaseInfoList());
        Integer fl = po.getWithFlags();
        int flags = fl != null ? fl : 0;
        SimpleUnsInstance instance = new SimpleUnsInstance(
                po.getId(),
                po.getPath(),
                po.getAlias(),
                po.getTableName(),
                po.getDataType(),
                po.getParentId(),
                StringUtils.isEmpty(po.getTableName()) && !Constants.withRetainTableWhenDeleteInstance(flags),
                Constants.withDashBoard(flags),
                po.getFields(),
                po.getName());
        instance.setFlags(po.getWithFlags());
        TreeMap<Long, String> labelIds = po.getLabelIds();
        instance.setLabelIds(labelIds != null && !labelIds.isEmpty() ? labelIds.keySet() : null);
        list.topics.put(instance.getId(), instance);
    }

    private void addPo4Remove(HashMap<SrcJdbcType, TopicBaseInfoList> typeListMap, CreateTopicDto po) {
        SrcJdbcType jdbcType = po.getDataSrcId();
        TopicBaseInfoList list = typeListMap.computeIfAbsent(jdbcType, k -> new TopicBaseInfoList());
        Integer fl = po.getFlags();
        int flags = fl != null ? fl : 0;
        SimpleUnsInstance instance = new SimpleUnsInstance(
                po.getId(),
                po.getPath(),
                po.getAlias(),
                po.getTableName(),
                po.getDataType(),
                po.getParentId(),
                StringUtils.isEmpty(po.getTableName()) && !Constants.withRetainTableWhenDeleteInstance(flags),
                Constants.withDashBoard(flags),
                po.getFields(),
                po.getName());
        instance.setFlags(po.getFlags());
        TreeMap<Long, String> labelIds = po.getLabelIds();
        instance.setLabelIds(labelIds != null && !labelIds.isEmpty() ? labelIds.keySet() : null);
        list.topics.put(instance.getId(), instance);
    }

    static class TopicBaseInfoList {
        Map<Long, SimpleUnsInstance> topics = new LinkedHashMap<>();
    }

    public static void batchRemoveExternalTopic(Collection<CreateTopicDto[]> topics) {
        for (CreateTopicDto[] arr : topics) {
            for (CreateTopicDto t : arr) {
                EXTERNAL_TOPIC_CACHE.remove(t.getTopic());
            }
        }
    }

}
