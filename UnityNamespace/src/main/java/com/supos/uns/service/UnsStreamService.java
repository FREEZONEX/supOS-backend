package com.supos.uns.service;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supos.common.Constants;
import com.supos.common.NodeType;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.StreamHandler;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import com.supos.common.dto.BaseResult;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.StreamInfo;
import com.supos.common.exception.BuzException;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.PaginationSearchResult;
import com.supos.uns.vo.StreamDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsStreamService {

    private StreamHandler streamHandler;
    @Autowired
    UnsMapper unsMapper;


    @Transactional(timeout = 300)
    public PaginationSearchResult<List<StreamDetail>> listStreams(String keyword, int pageNumber, int pageSize) {
        if ("".equals(keyword)) {
            keyword = null;
        }
        final int searchType = Constants.CALCULATION_HIST_TYPE;
        int total = unsMapper.countByDataType(keyword, searchType);
        if (pageNumber < 1) {
            pageNumber = 1;
        }
        final int offset = (pageNumber - 1) * pageSize;
        PageResultDTO page = new PageResultDTO();
        page.setPageNo(pageNumber);
        page.setTotal(total);
        page.setPageSize(pageSize);
        PaginationSearchResult<List<StreamDetail>> result = new PaginationSearchResult<>();
        result.setPage(page);
        List<StreamDetail> list = Collections.emptyList();
        if (total > 0) {
            final String kw = keyword != null ? "%" + keyword + "%" : null;
            ArrayList<UnsPo> poList = unsMapper.listByDataType(kw, searchType, offset, pageSize);
            if (poList == null || poList.isEmpty()) {
                page.setTotal(0);
                result.setMsg("DBListErr");
            } else {
                List<String> streamNames = poList.stream().map(p -> p.getAlias()).collect(Collectors.toList());
                List<StreamInfo> exists = streamHandler.listByNames(streamNames);
                if (exists == null) {
                    exists = Collections.emptyList();
                }
                int sizeExists = exists.size(), poSize = poList.size();
                if (sizeExists != poSize) {
                    log.warn("stream 数据不一致[exists={}, poSize={}]", sizeExists, poSize);

                    if (sizeExists < poSize) {// 需要删除 uns stream
                        HashSet<String> existsTopics = new HashSet<>();
                        for (StreamInfo streamInfo : exists) {
                            String steamTable = streamInfo.getTargetTable();
                            int str = steamTable.indexOf("/stream/");
                            if (str > 0) {
                                existsTopics.add(steamTable);
                            }
                        }
                        log.debug("existsTopics: {}", existsTopics);
                        ArrayList<String> delTopics = new ArrayList<>(poSize);
                        List<Long> toDelIds = poList.stream().filter(po -> !existsTopics.contains(po.getId())).map(po -> {
                            delTopics.add(po.getPath());
                            return po.getId();
                        }).collect(Collectors.toList());
                        if (toDelIds.size() > 0) {
                            QueryWrapper<UnsPo> query = new QueryWrapper<UnsPo>().eq("path_type", NodeType.InstanceForCalc.code);
                            query.in("id", toDelIds);
                            log.warn("stream 数据不一致[exists={}, poSize={}], 准备删除: {}", sizeExists, poSize, delTopics);
                            unsMapper.delete(query);

                            total = unsMapper.countByDataType(keyword, searchType);
                            page.setTotal(total);
                            poList = unsMapper.listByDataType(kw, searchType, offset, pageSize);
                        }
                    }
                }
                Map<String, StreamInfo> streamInfoMap = exists.stream().collect(Collectors.toMap(p -> p.getName(), p -> p));
                list = poList.stream().map(p -> {
                    StreamDetail detail = new StreamDetail();
                    detail.setNamespace(p.getPath());
                    detail.setDescription(p.getDescription());
                    Date ct = p.getCreateAt();
                    if (ct != null) {
                        detail.setCreateTime(ct.getTime());
                    }
                    StreamInfo info = streamInfoMap.get(p.getAlias());
                    if (info != null) {
                        detail.setStatus(info.getStatus());
                        detail.setSql(info.getSql());
                    }
                    return detail;
                }).collect(Collectors.toList());
            }
        }
        result.setData(list);
        return result;
    }

    StreamDetail po2stream(UnsPo p) {
        List<StreamInfo> exists = streamHandler.listByNames(Arrays.asList(p.getAlias()));
        if (CollectionUtil.isEmpty(exists)) {
            return null;
        }
        StreamInfo info = exists.get(0);
        StreamDetail detail = new StreamDetail();
        detail.setNamespace(p.getPath());
        detail.setDescription(p.getDescription());
        Date ct = p.getCreateAt();
        if (ct != null) {
            detail.setCreateTime(ct.getTime());
        }
        detail.setStatus(info.getStatus());
        detail.setSql(info.getSql());
        return detail;
    }

    @Transactional(timeout = 300)
    public BaseResult deleteStream(String namespace) {
        UnsPo po = getUnsStreamPO(namespace);
        if (po == null) {
            throw new BuzException("uns.stream.not.found");
        }
        unsMapper.deleteById(po.getId());

        String name = po.getDataPath();
        streamHandler.deleteStream(name);
        return new BaseResult();
    }

    public BaseResult stopStream(String namespace) {
        UnsPo po = getUnsStreamPO(namespace);
        if (po == null) {
            throw new BuzException("uns.stream.not.found");
        }
        String name = po.getAlias();
        streamHandler.stopStream(name);
        return new BaseResult();
    }

    public BaseResult resumeStream(String namespace) {
        UnsPo po = getUnsStreamPO(namespace);
        if (po == null) {
            throw new BuzException("uns.stream.not.found");
        }
        String name = po.getAlias();
        streamHandler.resumeStream(name);
        return new BaseResult();
    }

    private UnsPo getUnsStreamPO(String namespace) {
        if (StringUtils.isEmpty(namespace)) {
            throw new BuzException("uns.topic.empty");
        }
        UnsPo po = unsMapper.selectOne(new QueryWrapper<UnsPo>().eq("alias", namespace));
        return po;
    }

    @EventListener(classes = ContextRefreshedEvent.class)
    @Order
    public void init(ContextRefreshedEvent event) throws BeansException {
        Map<String, TimeSequenceDataStorageAdapter> adapterMap = event.getApplicationContext().getBeansOfType(TimeSequenceDataStorageAdapter.class);
        if (adapterMap != null && adapterMap.size() > 0) {
            for (TimeSequenceDataStorageAdapter adapter : adapterMap.values()) {
                SrcJdbcType jdbcType = adapter.getJdbcType();
                StreamHandler streamHandler = adapter.getStreamHandler();
                if (streamHandler != null && jdbcType.typeCode == Constants.TIME_SEQUENCE_TYPE) {
                    this.streamHandler = streamHandler;
                }
            }
            log.info("** streamHandler={}", streamHandler);
        }
    }
}
