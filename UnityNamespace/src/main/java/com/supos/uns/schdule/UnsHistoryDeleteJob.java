package com.supos.uns.schdule;

import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.config.ContainerInfo;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.SimpleUnsInstance;
import com.supos.common.event.*;
import com.supos.uns.dao.mapper.UnsHistoryDeleteJobMapper;
import com.supos.uns.dao.po.UnsHistoryDeleteJobPo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
@Slf4j
public class UnsHistoryDeleteJob {

    @Autowired
    private UnsHistoryDeleteJobMapper unsHistoryDeleteJobMapper;
    @Autowired
    private SystemConfig systemConfig;

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
        for (int i = 0; i < batches.size(); i++) {
            log.info("==>触发删除超期历史时序数据：{}, 批次：{}", batches.get(i), i);
            // 判断安装TDENGINE还是Timescale
            ContainerInfo tsdbContainer = systemConfig.getContainerMap().get("tsdb");
            if (tsdbContainer != null) {
                List<SimpleUnsInstance> standardList = new ArrayList<>();
                List<SimpleUnsInstance> nonStandardList = new ArrayList<>();
                fillList(batches.get(i), standardList, nonStandardList);
                EventBus.publishEvent(new RemoveTimeScaleTopicsEvent(this, standardList, nonStandardList));
            } else {
                EventBus.publishEvent(buildRemoveTDengineEvent(batches.get(i)));
            }
            List<String> aliasList = batches.get(i).stream().map(UnsHistoryDeleteJobPo::getAlias).toList();
            // 删除数据库存档
            unsHistoryDeleteJobMapper.deleteByAliases(aliasList);
        }

    }

    private RemoveTDengineEvent buildRemoveTDengineEvent(List<UnsHistoryDeleteJobPo> deleteUnsJobs) {
        Map<Long, SimpleUnsInstance> topics = new HashMap<>();
        Collection<String> aliasList = new ArrayList<>();
        for (UnsHistoryDeleteJobPo job : deleteUnsJobs) {
            SimpleUnsInstance sui = new SimpleUnsInstance();
            sui.setTableName(job.getTableName());
            sui.setAlias(job.getAlias());
            sui.setName(job.getName());
            sui.setFields(job.getFields());
            sui.setDataType(job.getDataType());
            sui.setPath(job.getPath());
            // 此ID非uns的数据ID
            topics.put(job.getId(), sui);
            aliasList.add(job.getAlias());
        }
        return new RemoveTDengineEvent(this, SrcJdbcType.TdEngine, topics, false, false, aliasList);
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
        if (event.jdbcType == null || event.jdbcType.typeCode != Constants.TIME_SEQUENCE_TYPE) {
            return;
        }
        long t0 = System.currentTimeMillis();
        Collection<SimpleUnsInstance> simpleUnsList = event.topics.values().stream().filter(t -> Constants.withHasData(t.getFlags())).toList();
        List<UnsHistoryDeleteJobPo> deleteUns = new ArrayList<>();
        for (SimpleUnsInstance simpleUns : simpleUnsList) {
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
            po.setTableName(simpleUns.getTableNameOnly());
            deleteUns.add(po);
            if (deleteUns.size() >= 500) {
                unsHistoryDeleteJobMapper.batchInsert(deleteUns);
                deleteUns.clear();
            }
        }
        if (!deleteUns.isEmpty()) {
            unsHistoryDeleteJobMapper.batchInsert(deleteUns);
            deleteUns.clear();
        }
        long t1 = System.currentTimeMillis();
        log.info("历史保存删除耗时 : {} ms, size={} of {}", t1 - t0, simpleUnsList.size(), event.topics.size());
    }

    private void fillList(List<UnsHistoryDeleteJobPo> jobs, List<SimpleUnsInstance> standardList, List<SimpleUnsInstance> nonStandardList) {
        for (UnsHistoryDeleteJobPo job : jobs) {
            if (StringUtils.hasText(job.getTableName())) {
                SimpleUnsInstance simpleUns = new SimpleUnsInstance();
                simpleUns.setAlias(job.getAlias());
                simpleUns.setTableName(job.getTableName());
                simpleUns.setId(job.getId());
                standardList.add(simpleUns);
            } else {
                SimpleUnsInstance simpleUns = new SimpleUnsInstance();
                simpleUns.setAlias(job.getAlias());
                simpleUns.setTableName(job.getAlias());
                nonStandardList.add(simpleUns);
            }
        }
    }

}
