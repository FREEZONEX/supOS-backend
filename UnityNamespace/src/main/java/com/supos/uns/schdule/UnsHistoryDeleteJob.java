package com.supos.uns.schdule;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.config.ContainerInfo;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.SimpleUnsInfo;
import com.supos.common.event.*;
import com.supos.uns.dao.mapper.UnsHistoryDeleteJobMapper;
import com.supos.uns.dao.po.UnsHistoryDeleteJobPo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UnsHistoryDeleteJob {

    @Autowired
    private UnsHistoryDeleteJobMapper unsHistoryDeleteJobMapper;
    @Autowired
    private SystemConfig systemConfig;

    boolean usTsdb;

    @EventListener(classes = ContextRefreshedEvent.class)
    void init() {
        ContainerInfo tsdbContainer = systemConfig.getContainerMap().get("tsdb");
        usTsdb = tsdbContainer != null;
    }

    /**
     * 每天晚上11点执行，检查是否超期要删除的uns时序数据
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void scanOverDueJob() {
        List<UnsHistoryDeleteJobPo> jobs = unsHistoryDeleteJobMapper.selectOverDue(Constants.UNS_OVERDUE_DELETE);
        if (jobs.isEmpty()) {
            return;
        }
        // 筛选出VQT数据
        List<List<UnsHistoryDeleteJobPo>> batches = Lists.partition(jobs, 200);

        int i = 0;
        for (List<UnsHistoryDeleteJobPo> part : batches) {
            log.info("==>触发删除超期历史时序数据：{}, 批次：{}", part, i++);
            // 判断安装TDENGINE还是Timescale
            if (usTsdb) {
                sendTsdbRemoveEvent(part);
            } else {
                EventBus.publishEvent(buildRemoveTDengineEvent(part));
            }
            List<String> aliasList = part.stream().map(UnsHistoryDeleteJobPo::getAlias).toList();
            // 删除数据库存档
            unsHistoryDeleteJobMapper.deleteByAliases(aliasList);
        }

    }

    private void sendTsdbRemoveEvent(List<? extends SimpleUnsInfo> part) {
        List<SimpleUnsInfo> standardList = new ArrayList<>();
        List<SimpleUnsInfo> nonStandardList = new ArrayList<>();
        fillList(part, standardList, nonStandardList);
        EventBus.publishEvent(new RemoveTimeScaleTopicsEvent(this, standardList, nonStandardList));
    }

    static final CopyOptions copyOptions = new CopyOptions().ignoreNullValue().ignoreError();

    private RemoveTDengineEvent buildRemoveTDengineEvent(List<UnsHistoryDeleteJobPo> deleteUnsJobs) {
        ArrayList<CreateTopicDto> topics = new ArrayList<>(deleteUnsJobs.size());
        for (UnsHistoryDeleteJobPo job : deleteUnsJobs) {
            CreateTopicDto dto = new CreateTopicDto();
            BeanUtil.copyProperties(job, dto, copyOptions);
            dto.setDataSrcId(SrcJdbcType.TdEngine);
            // 此ID非uns的数据ID
            topics.add(dto);
        }
        return new RemoveTDengineEvent(this, topics, false, false);
    }

    /**
     * 监听uns创建时间， 需要将删除时序数据的job删掉
     *
     * @param event
     */
    @EventListener(classes = BatchCreateTableEvent.class)
    @Order(1000)
    public void deleteHistoryUns(BatchCreateTableEvent event) {
        CreateTopicDto[] createTopicDtos = event.topics.get(SrcJdbcType.TimeScaleDB);
        if (createTopicDtos == null) {
            createTopicDtos = event.topics.get(SrcJdbcType.TdEngine);
        }

        if (createTopicDtos == null || createTopicDtos.length == 0) {
            return;
        }

        Map<String, FieldDefine[]> tmpMap = new HashMap<>();
        for (CreateTopicDto dto : createTopicDtos) {
            tmpMap.put(dto.getAlias(), dto.getFields());
            if (tmpMap.size() >= 500) {
                deleteHistoryUns(tmpMap);
                tmpMap.clear();
            }
        }
        if (!tmpMap.isEmpty()) {
            deleteHistoryUns(tmpMap);
            tmpMap.clear();
        }
    }

    private void deleteHistoryUns(Map<String, FieldDefine[]> tmpMap) {
        List<String> shouldDeletes = new ArrayList<>();
        List<UnsHistoryDeleteJobPo> pos = unsHistoryDeleteJobMapper.selectByAlias(tmpMap.keySet());
        for (UnsHistoryDeleteJobPo po : pos) {
            if (isSameUnsFieldDefine(tmpMap.get(po.getAlias()), po.getFields())) {
                shouldDeletes.add(po.getAlias());
            }
        }
        if (!shouldDeletes.isEmpty()) {
            unsHistoryDeleteJobMapper.deleteByAliases(shouldDeletes);
        }
    }

    private boolean isSameUnsFieldDefine(FieldDefine[] as, FieldDefine[] bs) {
        if (as.length != bs.length) {
            return false;
        }
        Map<String, String> aMap = new HashMap<>();
        for (FieldDefine a : as) {
            aMap.put(a.getName(), a.getType().getName());
        }
        for (FieldDefine b : bs) {
            String type = aMap.get(b.getName());
            if (!b.getType().getName().equals(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 监听uns删除事件， 保存删除的数据并保留7天历史数据
     *
     * @param event
     */
    @EventListener(RemoveTopicsEvent.class)
    @Order(1000)
    public void saveToHistoryDelete(RemoveTopicsEvent event) {
        long t0 = System.currentTimeMillis();
        Collection<CreateTopicDto> simpleUnsList = new LinkedList<>();
        ArrayList<CreateTopicDto> noDataList = new ArrayList<>(event.topics.size());
        for (CreateTopicDto t : event.topics) {
            if (t.getDataSrcId() != null && t.getDataSrcId().typeCode == Constants.TIME_SEQUENCE_TYPE) {
                if (Constants.withHasData(t.getFlags())) {
                    simpleUnsList.add(t);
                } else {
                    noDataList.add(t);
                }
            }
        }
        List<UnsHistoryDeleteJobPo> deleteUns = new ArrayList<>();
        for (CreateTopicDto simpleUns : simpleUnsList) {
            log.debug("simpleUns==> {}", simpleUns);
            UnsHistoryDeleteJobPo po = new UnsHistoryDeleteJobPo();
//            long id = IdUtil.getSnowflakeNextId();
            po.setId(simpleUns.getId());
            po.setName(simpleUns.getName());
            po.setPath(simpleUns.getPath());
            po.setFields(simpleUns.getFields());
            po.setPathType(Constants.PATH_TYPE_FILE);
            po.setAlias(simpleUns.getAlias());
            po.setDataType(simpleUns.getDataType());
            po.setTableName(simpleUns.getTableName());
            deleteUns.add(po);
            if (deleteUns.size() >= 500) {
                unsHistoryDeleteJobMapper.deleteByAliases(deleteUns.stream().map(UnsHistoryDeleteJobPo::getAlias).collect(Collectors.toList()));
                unsHistoryDeleteJobMapper.batchInsert(deleteUns);
                deleteUns.clear();
            }
        }
        if (!deleteUns.isEmpty()) {
            unsHistoryDeleteJobMapper.deleteByAliases(deleteUns.stream().map(UnsHistoryDeleteJobPo::getAlias).collect(Collectors.toList()));
            unsHistoryDeleteJobMapper.batchInsert(deleteUns);
            deleteUns.clear();
        }
        if (!noDataList.isEmpty()) {
            for (List<CreateTopicDto> part : Lists.partition(noDataList, 500)) {
                if (usTsdb) {
                    sendTsdbRemoveEvent(part);
                } else {
                    EventBus.publishEvent(new RemoveTDengineEvent(this, part, false, false));
                }
            }
        }
        long t1 = System.currentTimeMillis();
        log.info("历史保存删除耗时 : {} ms, size={}, noDataSize={}", t1 - t0, simpleUnsList.size(), noDataList.size());
    }

    private void fillList(List<? extends SimpleUnsInfo> jobs, List<SimpleUnsInfo> standardList, List<SimpleUnsInfo> nonStandardList) {
        for (SimpleUnsInfo job : jobs) {
            if (StringUtils.hasText(job.getTableName())) {
                standardList.add(job);
            } else {
                nonStandardList.add(job);
            }
        }
    }

}
