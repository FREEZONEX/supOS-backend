package com.supos.uns.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.UnsResultDto;
import com.supos.common.dto.UnsTreeCondition;
import com.supos.common.utils.LayRecUtil;
import com.supos.uns.dao.mapper.UnsLabelMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.util.PageUtil;
import com.supos.uns.util.UnsConverter;
import com.supos.uns.vo.TopicTreeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsTreeService {

    @Autowired
    UnsMapper unsMapper;
    @Autowired
    UnsDefinitionService unsDefinitionService;
    @Autowired
    UnsConverter unsConverter;
    @Autowired
    UnsLabelMapper unsLabelMapper;

    public PageResultDTO<TopicTreeResult> tree(UnsTreeCondition params) {
        String keyword = params.getKeyword();
        if (params.getSearchType() == 1 && (StringUtils.hasText(keyword) || params.getDataType() != null || params.getPathType() != null)) {
            return searchByType(params);
        } else if (params.getSearchType() == 2 || params.getSearchType() == 3) {
            return searchByType(params);
        } else {
            //懒加载模式 不带搜索条件
            return pageTreeResult(params);
        }
    }

    /**
     * 根据筛选条件和搜索类型 匹配搜索结果
     * @param params
     * @return
     */
    public UnsResultDto searchByDefinition(UnsTreeCondition params) {
        //从内存获取所有Uns定义 文件 + 文件夹
        List<CreateTopicDto> allDefinitions = unsDefinitionService.allDefinitions().stream()
                .filter(dto -> (Constants.PATH_TYPE_FILE == dto.getPathType() || Constants.PATH_TYPE_DIR == dto.getPathType())
                        && (Objects.isNull(dto.getDataType()) || Constants.ALARM_RULE_TYPE != dto.getDataType()))
                .collect(Collectors.toList());
        List<CreateTopicDto> allMatches = new ArrayList<>();
        //根据筛选条件筛选数据
        if (params.getSearchType() == 1) {
            allMatches = allDefinitions.stream().filter(dto -> {
                //父级id匹配
                boolean matchParentId;
                if (params.getParentId() == null) {
                    matchParentId = true;
                } else if (params.getParentId() == 0) { //顶级节点
                    matchParentId = dto.getParentId() == null;
                } else { // 根据parentId匹配
                    matchParentId = Objects.equals(params.getParentId(), dto.getParentId());
                }
                //关键字匹配
                boolean matchKeyword = true;
                if (StringUtils.hasText(params.getKeyword())) {
                    matchKeyword = dto.getAlias().contains(params.getKeyword()) || dto.getPath().contains(params.getKeyword());
                }
                //dataType匹配
                boolean matchDataType = true;
                if (params.getDataType() != null) {
                    matchDataType = Constants.PATH_TYPE_FILE == dto.getPathType() && dto.getDataType().equals(params.getDataType());
                }
                boolean matchPathType = true;
                if (params.getPathType() != null) {
                    matchPathType = dto.getPathType().equals(params.getPathType());
                }
                return matchParentId && matchKeyword && matchDataType && matchPathType;
            }).collect(Collectors.toList());
        } else if (params.getSearchType() == 2) {
            allMatches = unsLabelMapper.getUnsByKeyword(params.getKeyword()).stream().map(uns -> unsConverter.po2dto(uns)).collect(Collectors.toList());
        } else if (params.getSearchType() == 3) {
            allMatches = unsMapper.listInTemplate(params.getKeyword()).stream().map(uns -> unsConverter.po2dto(uns)).collect(Collectors.toList());
        }
        return new UnsResultDto(allDefinitions, allMatches);
    }

    public PageResultDTO<TopicTreeResult> pageTreeResult(UnsTreeCondition params) {
        UnsResultDto unsResultDto = searchByDefinition(params);

        List<CreateTopicDto> allDefinitions = unsResultDto.getAllDefinitions();
        List<CreateTopicDto> matchResults = unsResultDto.getMatchResults();

        // 构建 layRec 到所有子孙节点列表的映射
        Map<String, List<CreateTopicDto>> layRecToChildrenMap = LayRecUtil.buildParentToChildrenMap2(allDefinitions);

        matchResults.sort(Comparator.comparing(CreateTopicDto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        //手动分页
        List<CreateTopicDto> pageResult = ListUtil.page(params.getPageNo().intValue() - 1, params.getPageSize().intValue(), matchResults);

        //转换
        List<TopicTreeResult> treeResults = pageResult.stream().map(dto -> {
            TopicTreeResult topicTreeResult = unsConverter.dto2TreeResult(dto);
            if (Constants.PATH_TYPE_DIR == dto.getPathType()) {
                String layRec = dto.getLayRec();
                if (layRec != null) {
                    // 获取所有子孙节点
                    List<CreateTopicDto> allChildren = layRecToChildrenMap.getOrDefault(layRec, Collections.emptyList());
                    long fileCount = allChildren.stream()
                            .filter(child -> Constants.PATH_TYPE_FILE == child.getPathType())
                            .count();
                    topicTreeResult.setCountChildren((int) fileCount);
                    topicTreeResult.setHasChildren(!allChildren.isEmpty());
                } else {
                    topicTreeResult.setCountChildren(0);
                    topicTreeResult.setHasChildren(false);
                }
            }
            return topicTreeResult;
        }).collect(Collectors.toList());

        Page<TopicTreeResult> page = Page.of(params.getPageNo(), params.getPageSize(), matchResults.size());
        return PageUtil.build(page, treeResults);
    }

    public PageResultDTO<TopicTreeResult> searchByType(UnsTreeCondition params) {
        //搜索模式下：只返回parentId下的一级节点信息
        Long pageNo = params.getPageNo();
        Long pageSize = params.getPageSize();

        UnsTreeCondition condition = BeanUtil.copyProperties(params, UnsTreeCondition.class);
        condition.setParentId(null);//排除ParentId参数
        UnsResultDto unsResultDto = searchByDefinition(condition);

        List<CreateTopicDto> allDefinitions = unsResultDto.getAllDefinitions();
        List<CreateTopicDto> matchResults = unsResultDto.getMatchResults();

        if (CollectionUtils.isEmpty(matchResults)) {
            return PageUtil.empty(Page.of(pageNo, pageSize));
        }
        UnsPo parent = null;
        String parentLayRec = null;
        if (params.getParentId() != null) {
            parent = unsMapper.selectById(params.getParentId());
            if (parent != null) {
                parentLayRec = parent.getLayRec();
            }
        }
        //对比父级和当前节点的layRec ，获取父级节点的下一级节点layRec，如父级为空，则返回父级layRec
        Map<Long, List<CreateTopicDto>> nextNodeMap = new HashMap<>();
        for (CreateTopicDto uns : matchResults) {
            String nextNodeId = LayRecUtil.getNextNodeAfterBasePath2(parentLayRec, uns.getLayRec());
            if (StringUtils.hasText(nextNodeId)) {
                Long id = Long.valueOf(nextNodeId);
                nextNodeMap.computeIfAbsent(id, k -> new ArrayList<>());
                if (!Objects.equals(uns.getId(), id)) {
                    nextNodeMap.get(id).add(uns);
                }
            }
        }
        long t2 = System.currentTimeMillis();
        List<CreateTopicDto> filtered = allDefinitions.stream()
                .filter(dto -> nextNodeMap.containsKey(dto.getId()))
                .sorted(Comparator.comparing(CreateTopicDto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
        long t3 = System.currentTimeMillis();
        //手动分页
        List<CreateTopicDto> pageResult = ListUtil.page(pageNo.intValue() - 1, pageSize.intValue(), filtered);
        List<TopicTreeResult> treeResultList = pageResult.stream().map(dto -> {
            TopicTreeResult result = unsConverter.dto2TreeResult(dto);
            // 使用 nextNodeMap 中的 children
            List<CreateTopicDto> children = nextNodeMap.getOrDefault(dto.getId(), Collections.emptyList());
            // 分组统计各类型数量
            Map<Integer, Long> typeCountMap = children.stream()
                    .collect(Collectors.groupingBy(CreateTopicDto::getPathType, Collectors.counting()));
            long folderCount = typeCountMap.getOrDefault(0, 0L);
            long fileCount = typeCountMap.getOrDefault(2, 0L);
            result.setCountChildren(Math.toIntExact(fileCount));
            result.setHasChildren(folderCount > 0 || fileCount > 0);
            return result;
        }).collect(Collectors.toList());

        Page<TopicTreeResult> page = Page.of(pageNo, pageSize, filtered.size());
        return PageUtil.build(page, treeResultList);
    }
}
