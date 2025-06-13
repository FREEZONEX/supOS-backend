package com.supos.uns.config;

import cn.hutool.core.text.TextSimilarity;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.DataStorageAdapter;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import com.supos.common.config.SystemConfig;
import com.supos.uns.service.DataStorageServiceHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.comparator.ComparableComparator;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class DataStorageHelperBuilder {

    @Bean
    public DataStorageServiceHelper dataStorageServiceHelper(@Autowired SystemConfig systemConfig,
                                                             @Autowired ApplicationContext beanFactory) {
        // 找寻被启用的对应数据存储
        DataStorageAdapter relationDbEnabled = null;
        TimeSequenceDataStorageAdapter sequenceDbEnabled = null;
        Map<String, DataStorageAdapter> adapterMap = beanFactory.getBeansOfType(DataStorageAdapter.class);
        Set<String> containers = systemConfig.getContainerMap().keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
        if (!adapterMap.isEmpty() && !containers.isEmpty()) {
            double reScore = 0, seqScore = 0;
            for (DataStorageAdapter adapter : adapterMap.values()) {
                SrcJdbcType jdbcType = adapter.getJdbcType();
                Optional<Double> scoreOp = containers.stream().map(s -> Math.max(TextSimilarity.similar(s, jdbcType.dataSrcType.toLowerCase()),
                        TextSimilarity.similar(s, jdbcType.name().toLowerCase()))).max(new ComparableComparator<>());
                final double score = scoreOp.isPresent() && scoreOp.get() > 0 ? scoreOp.get() : 0;
                switch (jdbcType.typeCode) {
                    case Constants.RELATION_TYPE: {
                        if (relationDbEnabled == null || score > reScore) {
                            relationDbEnabled = adapter;
                        }
                        break;
                    }
                    case Constants.TIME_SEQUENCE_TYPE: {
                        if ((sequenceDbEnabled == null || score > seqScore) && adapter instanceof TimeSequenceDataStorageAdapter) {
                            sequenceDbEnabled = (TimeSequenceDataStorageAdapter) adapter;
                        }
                        break;
                    }
                }
                log.info("jdbcType: {}, score={}", jdbcType, score);
            }
        }
        DataStorageServiceHelper storageServiceHelper = new DataStorageServiceHelper(relationDbEnabled, sequenceDbEnabled);
        log.info("containers: {}", containers);
        if (relationDbEnabled != null && sequenceDbEnabled != null) {
            log.info("关系库: {}, 时序库:{}", relationDbEnabled.getJdbcType(), sequenceDbEnabled.getJdbcType());
        }
        return storageServiceHelper;
    }
}
