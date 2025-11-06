package com.supos.uns.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.http.ssl.DefaultSSLFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.NodeType;
import com.supos.common.SrcJdbcType;
import com.supos.common.annotation.DateTimeConstraint;
import com.supos.common.dto.*;
import com.supos.common.dto.protocol.RestServerConfigDTO;
import com.supos.common.enums.FieldType;
import com.supos.common.event.*;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.sdk.UnsQueryApi;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.*;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.dao.mapper.AlarmHandlerMapper;
import com.supos.uns.dao.mapper.AlarmMapper;
import com.supos.uns.dao.mapper.UnsLabelMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.dto.ExternalTopicCacheDto;
import com.supos.uns.util.PageUtil;
import com.supos.uns.util.ParserUtil;
import com.supos.uns.util.UnsConverter;
import com.supos.uns.util.UnsCountCache;
import com.supos.uns.vo.*;
import com.supos.ws.WebsocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsQueryService implements UnsQueryApi {

    private final UnsMapper unsMapper;
    private final AlarmMapper alarmMapper;
    private final UnsLabelMapper unsLabelMapper;
    private final AlarmHandlerMapper alarmHandlerMapper;
    private final UnsLabelService unsLabelService;
    @Autowired
    IUnsDefinitionService unsDefinitionService;
    @Autowired
    private WebsocketSessionManager wssm;


    @Autowired
    UnsCountCache unsCountCache;

    // 缓存外部topic  <topic,payload>
    public static Map<String, ExternalTopicCacheDto> EXTERNAL_TOPIC_CACHE = new ConcurrentHashMap<>();

    private static final List<String> fieldTypes =
            Collections.unmodifiableList(Arrays.stream(FieldType.values()).map(FieldType::getName).collect(Collectors.toList()));

    public UnsQueryService(@Autowired UnsMapper unsMapper, @Autowired AlarmMapper alarmMapper,
                           @Autowired UnsLabelMapper unsLabelMapper,
                           @Autowired AlarmHandlerMapper alarmHandlerMapper,
                           @Autowired UnsLabelService unsLabelService) {
        this.unsMapper = unsMapper;
        this.alarmMapper = alarmMapper;
        this.unsLabelMapper = unsLabelMapper;
        this.alarmHandlerMapper = alarmHandlerMapper;
        this.unsLabelService = unsLabelService;
    }

    public JsonResult<Collection<String>> listTypes() {
        return new JsonResult<Collection<String>>().setData(fieldTypes);
    }

    public Set<String> listByAlias(Collection<String> alias) {
        return unsMapper.selectList(new QueryWrapper<UnsPo>().in("alias", alias).select("alias"))
                .stream().map(UnsPo::getPath).collect(Collectors.toSet());
    }

    public long countByAlias(Collection<String> alias) {
        long count = 0;
        if (!CollectionUtils.isEmpty(alias)) {
            List<List<String>> aliasList = Lists.partition(new ArrayList<>(alias), 2000);
            for (List<String> subAlias : aliasList) {
                count += unsMapper.selectCount(new QueryWrapper<UnsPo>().in("alias", subAlias));
            }
        }
        return count;
    }

    public Set<Long> listInstances(Collection<Long> instanceIds) {
        return unsMapper.listInstanceIds(instanceIds);
    }

    public static JsonResult<List<OuterStructureVo>> parseJson2uns(String json) {
        Object vo;
        try {
            vo = JsonUtil.fromJson(json);
        } catch (Exception e) {
            return new JsonResult<>(400, I18nUtils.getMessage("uns.invalid.json"));
        }
        FindDataListUtils.SearchResult searchResult = FindDataListUtils.findMultiDataList(vo, null);
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.multiResults)) {
            return new JsonResult<>(400, I18nUtils.getMessage("uns.rest.data404"));
        }
        LinkedList<FindDataListUtils.ListResult> onlyLists = new LinkedList<>();
        for (FindDataListUtils.ListResult rs : searchResult.multiResults) {
            if (rs.dataInList) {
                onlyLists.add(rs);
            }
        }
        Collection<FindDataListUtils.ListResult> list;
        if (!onlyLists.isEmpty()) {// 数组非空，就只要数组
            list = onlyLists;
        } else {// 否则取所有结果，即所有对象
            list = searchResult.multiResults;
        }
        List<OuterStructureVo> rsList = list.stream().map(UnsQueryService::map2fields).collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(OuterStructureVo::getDataPath))),
                ArrayList::new
        ));
        return new JsonResult<>(0, "ok", rsList);
    }

    private static OuterStructureVo map2fields(FindDataListUtils.ListResult rs) {
        LinkedHashMap<String, FieldType> fieldTypes = new LinkedHashMap<>();
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (Map<String, Object> data : rs.list) {
            for (Map.Entry<String, Object> m : data.entrySet()) {
                String k = m.getKey();
                Object v = m.getValue();
                FieldType fieldType = fieldTypes.get(k);
                FieldType guessType = guessType(v);
                if (fieldType == null || guessType.ordinal() > fieldType.ordinal()) {
                    fieldTypes.put(k, guessType);
                    values.put(k, v);
                }
            }
        }
        if (fieldTypes.isEmpty()) {
            return null;
        }
        List<FieldDefine> fields = fieldTypes.entrySet().stream()
                .map(e -> new FieldDefine(e.getKey(), e.getValue()))
                .toList();
        return new OuterStructureVo(rs.dataPath, fields, values);
    }

    public static FieldType guessType(Object o) {
        if (o != null) {
            Class clazz = o.getClass();
            if (clazz == Integer.class) {
                return FieldType.INTEGER;
            } else if (clazz == Long.class) {
                return FieldType.LONG;
            } else if (clazz == Double.class || clazz == BigDecimal.class) {
                return FieldType.DOUBLE;
            } else if (clazz == Boolean.class) {
                return FieldType.BOOLEAN;
            } else if (DateTimeConstraint.parseDate(o.toString()) != null) {
                return FieldType.DATETIME;
            }
        }
        return FieldType.STRING;
    }

    public JsonResult parseJson2TreeUns(String json) {
        return new JsonResult<>(0, "ok", ParserUtil.parserJson2Tree(json));
    }

    public TopicPaginationSearchResult searchPaged(String modelTopic, String keyword, NodeType searchType, Set<Integer> dataTypes, int pageNumber, int pageSize, Integer minNumFields) {
        if ("".equals(keyword)) {
            keyword = null;
        }
        if (keyword != null) {
            keyword = keyword.replace("_", "\\_").replace("%", "\\%");
            keyword = "%" + keyword + "%";
        }
        TopicPaginationSearchResult result = new TopicPaginationSearchResult();
        if (searchType == NodeType.Path || searchType == NodeType.Instance) {
            if ("".equals(modelTopic)) {
                modelTopic = null;
            }
            if (searchType == NodeType.Path) {
                dataTypes = null;
            }
            String modelId = null;
/*            if (modelTopic != null && !modelTopic.isEmpty()) {
                char ec = modelTopic.charAt(modelTopic.length() - 1);
                if (ec != '*') {
                    if (ec == '/') {
                        modelTopic = modelTopic + '*';
                    } else {
                        modelTopic = modelTopic + "/*";
                    }
                }
                modelId = genIdForPath(modelTopic);
            }*/
            if (dataTypes != null && dataTypes.isEmpty()) {
                dataTypes = null;
            }
            int total = unsMapper.countPaths(modelId, keyword, searchType.code, dataTypes);
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            int offset = (pageNumber - 1) * pageSize;
            PageResultDTO page = new PageResultDTO();
            page.setPageNo(pageNumber);
            page.setTotal(total);
            page.setPageSize(pageSize);
            result.setPage(page);
            if (total > 0) {
                ArrayList<SimpleUns> paths = unsMapper.listPaths(modelId, keyword, searchType.code, dataTypes, offset, pageSize);
                if (!CollectionUtils.isEmpty(paths)) {
                    for (SimpleUns simpleUns : paths) {
                        String path = simpleUns.getPath();
                        if (path.endsWith("/")) {
                            simpleUns.setPath(path.substring(0, path.length() - 1));
                        }
                        simpleUns.setTopic(Constants.useAliasAsTopic ? simpleUns.getAlias() : simpleUns.getPath());
                    }
                }
                List strList = paths;
                result.setData(strList);
            } else {
                result.setData(Collections.emptyList());
            }
        } else if (searchType == NodeType.InstanceForCalc) {
            if (minNumFields == null) {
                minNumFields = 1;
            }
            int total = unsMapper.countNotCalcSeqInstance(keyword, minNumFields);
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            int offset = (pageNumber - 1) * pageSize;
            PageResultDTO page = new PageResultDTO();
            page.setPageNo(pageNumber);
            page.setTotal(total);
            page.setPageSize(pageSize);
            result.setPage(page);
            if (total > 0) {
                ArrayList<UnsPo> list = unsMapper.listNotCalcSeqInstance(keyword, minNumFields, offset, pageSize);
                ArrayList<CalcInstanceSearchResult> rs = new ArrayList<>(list.size());
                for (UnsPo po : list) {
                    FieldDefine[] fs = po.getFields();
                    List<FieldDefineVo> fields = Arrays.stream(fs)
                            .filter(f -> (f.getType().isNumber || f.getType() == FieldType.BOOLEAN) && !f.isSystemField())
                            .map(f -> new FieldDefineVo(f.getName(), f.getType().name)).collect(Collectors.toList());
                    if (fields.size() > 0) {
                        CalcInstanceSearchResult srs = new CalcInstanceSearchResult();
                        srs.setId(po.getId().toString());
                        srs.setName(po.getName());
                        srs.setPath(po.getPath());
                        srs.setFields(fields);
                        srs.setDataType(po.getDataType());
                        srs.setParentDataType(po.getParentDataType());
                        rs.add(srs);
                    }
                }
                List objList = rs;
                result.setData(objList);
            } else {
                result.setData(Collections.emptyList());
            }
        } else if (searchType == NodeType.InstanceForTimeseries) {
            int total = unsMapper.countTimeSeriesInstance(keyword);
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            int offset = (pageNumber - 1) * pageSize;
            PageResultDTO page = new PageResultDTO();
            page.setPageNo(pageNumber);
            page.setTotal(total);
            page.setPageSize(pageSize);
            result.setPage(page);
            if (total > 0) {
                ArrayList<UnsPo> list = unsMapper.listTimeSeriesInstance(keyword, offset, pageSize);
                ArrayList<TimeseriesInstanceSearchResult> rs = new ArrayList<>(list.size());
                for (UnsPo po : list) {
                    FieldDefine[] fs = po.getFields();

                    LinkedList<FieldDefine> fsList = new LinkedList<>(Arrays.asList(fs));
                    Iterator<FieldDefine> itr = fsList.iterator();
                    CreateTopicDto dtp = UnsConverter.po2dto(po);
                    String tbF = dtp.getTbFieldName();
                    if (tbF != null) {
                        FieldDefine dvf = dtp.getFieldDefines().getFieldsMap().get(tbF);
                        FieldDefine vf = dtp.getFieldDefines().getFieldsMap().get(Constants.SYSTEM_SEQ_VALUE);
                        vf.setName(dvf.getTbValueName());
                    }
                    while (itr.hasNext()) {
                        FieldDefine fd = itr.next();
                        String name = fd.getName();
                        if (name.startsWith(Constants.SYSTEM_FIELD_PREV) || fd.getTbValueName() != null || name.startsWith(Constants.SYS_FIELD_CREATE_TIME)) {
                            itr.remove();
                        }
                    }
                    List<FieldDefine> fields = List.of(fsList.toArray(new FieldDefine[0]));
                    if (fields.size() > 0) {
                        TimeseriesInstanceSearchResult srs = new TimeseriesInstanceSearchResult();
                        srs.setId(po.getId().toString());
                        srs.setName(po.getName());
                        srs.setPath(po.getPath());
                        srs.setFields(fields);
                        srs.setParentDataType(po.getParentDataType());
                        rs.add(srs);
                    }
                }
                List objList = rs;
                result.setData(objList);
            } else {
                result.setData(Collections.emptyList());
            }
        } else if (searchType == NodeType.AlarmRule) {
            int total = unsMapper.countAlarmRules(keyword);
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            int offset = (pageNumber - 1) * pageSize;
            PageResultDTO page = new PageResultDTO();
            page.setPageNo(pageNumber);
            page.setTotal(total);
            page.setPageSize(pageSize);
            result.setPage(page);
            if (total > 0) {
                ArrayList<UnsPo> list = unsMapper.listAlarmRules(keyword, offset, pageSize);
                ArrayList<AlarmRuleSearchResult> rs = new ArrayList<>(list.size());
                Map<Long, Long[]> countAlarms = new HashMap<>(Math.max(64, list.size()));
                if (!CollectionUtils.isEmpty(list)) {
                    Collection<Long> unsIds = list.stream().map(UnsPo::getId).collect(Collectors.toSet());
                    final String UNS = AlarmRuleDefine.FIELD_UNS_ID;
                    List<AlarmPo> alarmPos = alarmMapper.selectList(new QueryWrapper<AlarmPo>()
                            .select(UNS, "count(1) as currentValue,SUM(CASE WHEN read_status = false THEN 1 ELSE 0 END) AS noReadCount").groupBy(UNS)
                            .in(UNS, unsIds));
                    for (AlarmPo po : alarmPos) {
                        countAlarms.put(po.getUns(), new Long[]{po.getCurrentValue().longValue(), po.getNoReadCount()});
                    }
                }
                for (UnsPo po : list) {
                    AlarmRuleSearchResult srs = new AlarmRuleSearchResult();
                    srs.setId(po.getId().toString());
                    srs.setTopic(po.getPath());
                    srs.setName(po.getName());// 告警名称改用 name
                    srs.setDescription(po.getDescription());
                    srs.setWithFlags(AlarmService.checkWithFlags(po.getWithFlags()));
                    srs.setHandlerList(Collections.emptyList());
                    if (srs.getWithFlags() == Constants.UNS_FLAG_ALARM_ACCEPT_PERSON) {
                        srs.setHandlerList(alarmHandlerMapper.getByUnsId(po.getId()));
                    } else if (srs.getWithFlags() == Constants.UNS_FLAG_ALARM_ACCEPT_WORKFLOW) {
                        //TODO
//                        srs.setProcessDefinition(processService.getById(Long.valueOf(po.getExtend())));
                    }
                    Long[] counts = countAlarms.get(po.getId());
                    if (ObjectUtil.isNotNull(counts)) {
                        Long count = counts[0];
                        Long noReadCount = counts[1];
                        srs.setAlarmCount(count != null ? count.longValue() : 0);
                        srs.setNoReadCount(noReadCount != null ? noReadCount.longValue() : 0);
                    }
                    InstanceField[] refers = po.getRefers();
                    if (ObjectUtil.isNotNull(refers)) {
                        srs.setRefUns(String.valueOf(refers[0].getId()));
                        srs.setField(refers[0].getField());
                    }
                    AlarmRuleDefine ruleDefine = JsonUtil.fromJson(po.getProtocol(), AlarmRuleDefine.class);
                    ruleDefine.parseExpression(po.getExpression());
                    srs.setAlarmRuleDefine(ruleDefine);
                    srs.setParentDataType(po.getParentDataType());
                    rs.add(srs);
                }
                List objList = rs;
                result.setData(objList);
            } else {
                result.setData(Collections.emptyList());
            }
        }
        return result;
    }

    public JsonResult<List<TopicTreeResult>> searchByTag(String keyword) {
        List<UnsPo> allNamespaces = unsLabelMapper.getUnsByKeyword(keyword);
        List<TopicTreeResult> treeResults = new ArrayList<>(allNamespaces.size());
        treeResults = allNamespaces.stream().filter(uns -> {
            if (StringUtils.hasText(keyword)) {
                String name = PathUtil.getName(uns.getPath());
                if (name.toLowerCase().contains(keyword.toLowerCase()) || uns.getAlias().toLowerCase().contains(keyword.toLowerCase())) {
                    return true;
                } else {
                    return false;
                }
            }
            return true;
        }).map(uns -> {
            TopicTreeResult result = new TopicTreeResult();
            result.setId(String.valueOf(uns.getId()));
            result.setPathType(2);
            result.setProtocol(uns.getProtocolType());
            result.setDataType(uns.getDataType());
            result.setParentDataType(uns.getParentDataType());
            result.setPath(uns.getPath());
            result.setName(PathUtil.getName(uns.getPath()));
            result.setMount(parseMountDetail(uns, true));
            return result;
        }).collect(Collectors.toList());
        return new JsonResult<>(0, "ok", treeResults);
    }

    public JsonResult<List<TopicTreeResult>> searchByTemplate(String keyword) {
        List<TopicTreeResult> treeResults = new ArrayList<>();

        List<UnsPo> allNamespaces = unsMapper.listInTemplate(keyword);
        if (!CollectionUtils.isEmpty(allNamespaces)) {
            treeResults = new ArrayList<>(allNamespaces.size());
            for (UnsPo uns : allNamespaces) {
                String name = PathUtil.getName(uns.getPath());
                if (StringUtils.hasText(keyword)) {
                    if (!name.toLowerCase().contains(keyword.toLowerCase()) && !uns.getAlias().toLowerCase().contains(keyword.toLowerCase())) {
                        continue;
                    }
                }
                TopicTreeResult result = new TopicTreeResult();
                result.setId(String.valueOf(uns.getId()));
                result.setPathType(uns.getPathType());
                result.setProtocol(uns.getProtocolType());
                result.setPath(uns.getPath());
                result.setName(name);
                result.setMount(parseMountDetail(uns, true));
                result.setDataType(uns.getDataType());
                result.setParentDataType(uns.getParentDataType());
                treeResults.add(result);
            }
        }
        return new JsonResult<>(0, "ok", treeResults);
    }

    public JsonResult<List<TopicTreeResult>> searchTree(String keyword, Long parentId, boolean showRec, Integer pathType) {
        UnsSearchCondition condition = new UnsSearchCondition();
        condition.setKeyword(keyword);
        condition.setParentId(parentId);
        condition.setShowRec(showRec);
        condition.setPageSize(Long.MAX_VALUE);
        condition.setPathType(pathType);
        List<TopicTreeResult> topicTreeResults = searchTreeByCondition(condition).getData();
        return new JsonResult<>(0, "ok", topicTreeResults);
    }

    /**
     * UNS树搜索模式
     * 返回搜索结果后的树结构
     *
     * @param params
     * @return
     */
    public PageResultDTO<TopicTreeResult> unsSearchByTree(UnsSearchCondition params) {
        //搜索模式下：只返回parentId下的一级节点信息
        Long pageNo = params.getPageNo();
        Long pageSize = params.getPageSize();
        UnsSearchCondition searchCondition = new UnsSearchCondition();
        searchCondition.setKeyword(params.getKeyword());
        List<UnsPo> unsSearchResult = null;
        //根据条件全局搜索结果 查询类型：1-UNS（名称+别名） 2-含标签 3-含模板
        if (params.getSearchType() == 1) {
            unsSearchResult = unsMapper.listByConditions(searchCondition);
        } else if (params.getSearchType() == 2) {
            unsSearchResult = unsLabelMapper.getUnsByKeyword(params.getKeyword());
        } else if (params.getSearchType() == 3) {
            unsSearchResult = unsMapper.listInTemplate(params.getKeyword());
        }
        if (CollectionUtils.isEmpty(unsSearchResult)) {
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
        //查找父级下的直接子节点
        HashMap<Long, UnsPo> nextNode = new HashMap<>(unsSearchResult.size());
        for (UnsPo uns : unsSearchResult) {
            String nextNodeId = PathUtil.getNextNodeAfterBasePath(parentLayRec, uns.getLayRec());
            if (StringUtils.hasText(nextNodeId)) {
                nextNode.put(Long.valueOf(nextNodeId), uns);
            }
        }
        List<TopicTreeResult> filtered = new ArrayList<>();
        //如果父级下没有直接的子节点了，根据父级ID匹配搜索结果，并返回
        if (MapUtils.isEmpty(nextNode)) {
            filtered = unsSearchResult.stream().filter(search -> {
                //根据layRec搜索，并排除自身
                return search.getLayRec().contains(params.getParentId().toString()) && !params.getParentId().equals(search.getId());
            }).map(this::unsPoTrans2TreeResult).collect(Collectors.toList());
        } else {
            //key:id  value：children 所有的子数量(文件+文件夹)
            Map<Long, List<UnsPo>> idToChildren = new HashMap<>(nextNode.size());
//            List<String> searchResultLayRecList = unsSearchResult.stream().map(UnsPo::getLayRec).collect(Collectors.toList());
            //遍历所有的一级节点，和searchResultLayRecList进行筛选，查询出所有子孙节点的数量
            for (Long nextNodeId : nextNode.keySet()) {
                List<UnsPo> children = unsSearchResult.stream()
                        .filter(node -> {
                            String nodeId = node.getLayRec();
                            //查找匹配的节点，排除自身
                            return nodeId.contains(nextNodeId.toString()) && !nodeId.endsWith(nextNodeId.toString());
                        }).collect(Collectors.toList());
                idToChildren.put(nextNodeId, children);
            }
            filtered = unsMapper.listUnsByIds(nextNode.keySet()).stream()
                    .map(po -> {
                        TopicTreeResult result = unsPoTrans2TreeResult(po);
                        //获取所有子节点（文件+文件夹），分组统计各个数量，setCountChildren 和HasChildren
                        List<UnsPo> children = idToChildren.getOrDefault(po.getId(), Collections.emptyList());
                        Map<Integer, Long> typeCountMap = children.stream()
                                .collect(Collectors.groupingBy(UnsPo::getPathType, Collectors.counting()));
                        long folderCount = typeCountMap.getOrDefault(0, 0L);
                        long fileCount = typeCountMap.getOrDefault(2, 0L);
                        result.setCountChildren(Math.toIntExact(fileCount));
                        result.setHasChildren(folderCount > 0 || fileCount > 0);//如果存在子文件夹 或者存在子文件
                        return result;
                    })
                    .collect(Collectors.toList());
        }
        //手动分页
        List<TopicTreeResult> pageResult = ListUtil.page(pageNo.intValue() - 1, pageSize.intValue(), filtered);
        Page<TopicTreeResult> page = Page.of(pageNo, pageSize, filtered.size());
        return PageUtil.build(page, pageResult);
    }

    public PageResultDTO<TopicTreeResult> unsTree(UnsSearchCondition params) {
        String keyword = params.getKeyword();
        if (params.getSearchType() == 1 && StringUtils.hasText(keyword)) {
            return unsSearchByTree(params);
        } else if (params.getSearchType() == 2 || params.getSearchType() == 3) {
            return unsSearchByTree(params);
        } else {
            //懒加载模式 不带搜索条件
            Page<UnsPo> page = new Page<>(params.getPageNo(), params.getPageSize());
            int deep = ObjectUtil.defaultIfNull(params.getDeep(), 0);
            IPage<UnsPo> iPage = unsMapper.pageListByLazy(page, params);
            List<TopicTreeResult> treeResults = getTopicTreeResults(deep, iPage.getRecords());
            return PageUtil.build(iPage, treeResults);
        }
    }

    @NotNull
    private List<TopicTreeResult> getTopicTreeResults(int deep, List<UnsPo> unsList) {
        List<TopicTreeResult> treeResults = unsList.stream().map(uns -> {
            TopicTreeResult result = unsPoTrans2TreeResult(uns, true);
            if (deep == 0) {
                return result;
            } else {
                UnsSearchCondition condition = new UnsSearchCondition();
                condition.setParentId(uns.getId());
                condition.setDeep(deep > 0 ? deep - 1 : -1);
                condition.setPageSize(Long.MAX_VALUE);
                PageResultDTO<TopicTreeResult> childrenResult = unsTree(condition);
                if (!CollectionUtils.isEmpty(childrenResult.getData())) {
                    result.setChildren(childrenResult.getData());
                    result.setHasChildren(true);
                }
            }
            return result;
        }).collect(Collectors.toList());
        return treeResults;
    }

    @NotNull
    public TopicTreeResult unsPoTrans2TreeResult(UnsPo uns, boolean fromCache) {
        TopicTreeResult result = new TopicTreeResult();
        result.setId(uns.getId().toString());
        result.setAlias(uns.getAlias());
        if (uns.getParentId() != null) {
            result.setParentId(String.valueOf(uns.getParentId()));
        }
        result.setParentAlias(uns.getParentAlias());
        result.setPathType(uns.getPathType());
        result.setDataType(uns.getDataType());
        String name = PathUtil.getName(uns.getPath());
        result.setName(name);
        result.setDisplayName(uns.getDisplayName());
        result.setDescription(uns.getDescription());
        result.setPath(uns.getPath());
        result.setTemplateAlias(uns.getTemplateAlias());
        result.setPathName(PathUtil.getName(uns.getPath()));
        result.setExtend(uns.getExtend());
        result.setCreateAt(uns.getCreateAt());
        result.setUpdateAt(uns.getUpdateAt());
        result.setMount(parseMountDetail(uns, true));

        //从缓存中获取count
        if (fromCache) {
            UnsCountDTO count = unsCountCache.get(uns.getId());
            if (count == null) {
                int children = unsMapper.countAllChildrenByLayRec(uns.getLayRec());
                int direct = unsMapper.countDirectChildrenByParentId(uns.getId());
                count = new UnsCountDTO(children, direct);
                unsCountCache.put(uns.getId(), count);
            }
            result.setCountChildren(count.getCountChildren());
            result.setHasChildren(count.getCountDirectChildren() > 0);
        }
        return result;
    }

    @NotNull
    public TopicTreeResult unsPoTrans2TreeResult(UnsPo uns) {
        return unsPoTrans2TreeResult(uns, false);
    }

    public PageResultDTO<TopicTreeResult> searchTreeByCondition(UnsSearchCondition params) {
        Page<UnsPo> page = new Page<>(params.getPageNo(), params.getPageSize());
        IPage<UnsPo> iPage = unsMapper.pageListByConditions(page, params);
        if (CollectionUtils.isEmpty(iPage.getRecords())) {
            return PageUtil.build(iPage, Collections.emptyList());
        }
        //查询符合条件的记录
        List<UnsPo> list = iPage.getRecords();
        Set<String> layRecList = new HashSet<>();
        //通过layRec 找出所有的父节点包含自身
        list.forEach(uns -> {
            String layRec = uns.getLayRec();
            int lastSlash = layRec.lastIndexOf('/');
            if (lastSlash == -1) {//如果是顶级节点，增加自身
                layRecList.add(layRec);
            } else {
                String layRecSub = layRec.substring(0, lastSlash);//如果有父节点，排除父节点
                if (layRecSub.contains("/")) {
                    layRecList.addAll(Arrays.asList(layRecSub.split("/")));//把子节点ID都加入
                } else {
                    layRecList.add(layRecSub);
                }
            }
        });
        //父级点+自身
        Set<Long> layRecLongSet = layRecList.stream().map(Long::valueOf).collect(Collectors.toSet());
        //合并父节点 + 搜索节点 = ALL
        List<UnsPo> allNamespaces = unsMapper.selectByIds(layRecLongSet);
        allNamespaces.addAll(list);
        List<TopicTreeResult> treeResults = getTopicTreeResults(allNamespaces, list, params.getShowRec());
        return PageUtil.build(iPage, treeResults);
    }

    public JsonResult<InstanceDetail> getInstanceDetail(Long id, String alias) {
        InstanceDetail dto = new InstanceDetail();
        UnsPo file = null;
        if (ObjectUtil.isNotNull(id)) {
            file = unsMapper.selectById(id);
        } else if (alias != null) {
            file = unsMapper.selectOne(new LambdaQueryWrapper<UnsPo>()
                    .eq(UnsPo::getPathType, Constants.PATH_TYPE_FILE)
                    .eq(UnsPo::getAlias, alias));
        }
        if (null == file) {
            return new JsonResult<>(0, I18nUtils.getMessage("uns.file.not.found"), dto);
        }

        String expression = file.getExpression();
        InstanceField[] fs = file.getRefers();
        UnsPo origPo = null;//被引用的uns
        if (ArrayUtils.isNotEmpty(fs)) {
            if (file.getDataType() == Constants.CITING_TYPE) {
                Long origId = fs[0].getId();
                UnsPo orig = unsMapper.selectById(origId);
                if (orig != null) {
                    origPo = orig;
                    file.setFields(orig.getFields());
                    file.setWithFlags(orig.getWithFlags());
                }
            }
            List<Long> ids = Arrays.stream(fs).map(InstanceField::getId).toList();
            Map<Long, UnsPo> unsMap = unsMapper.listInstanceByIds(ids).stream().collect(Collectors.toMap(UnsPo::getId, k -> k));
            InstanceFieldVo[] refers = Arrays.stream(fs).map(field -> {
                InstanceFieldVo instanceFieldVo = new InstanceFieldVo();
                instanceFieldVo.setId(field.getId().toString());
                instanceFieldVo.setField(field.getField());
                instanceFieldVo.setUts(field.getUts());
                UnsPo ref = unsMap.get(field.getId());
                if (ref != null) {
                    instanceFieldVo.setAlias(ref.getAlias());
                    instanceFieldVo.setPath(ref.getPath());
                }
                return instanceFieldVo;
            }).toArray(InstanceFieldVo[]::new);
            dto.setRefers(refers);
            Map<String, Object> protocolMap = dto.getProtocol();
            Object whereExpr;
            if (expression != null) {
                Map<String, String> varReplacer = new HashMap<>(8);
                Map<String, String> showVarReplacer = new HashMap<>(8);
                for (int i = 0; i < fs.length; i++) {
                    InstanceField field = fs[i];
                    if (field != null) {
                        Long citingId = field.getId();
                        if (citingId != null) {
                            String var = Constants.VAR_PREV + (i + 1);
                            varReplacer.put(var, String.format("$\"%s\".%s#", citingId, field.getField()));
                            CreateTopicDto citingInfo = unsDefinitionService.getDefinitionById(citingId);
                            if (citingInfo != null) {
                                showVarReplacer.put(var, String.format("$\"%s\".%s#", citingInfo.getPath(), field.getField()));
                            }
                        }
                    }
                }
                if (!varReplacer.isEmpty()) {
                    String showExpression = ExpressionUtils.replaceExpression(expression, showVarReplacer);
                    dto.setShowExpression(showExpression);
                    expression = ExpressionUtils.replaceExpression(expression, varReplacer);
                }
            } else if (protocolMap != null && (whereExpr = protocolMap.get("whereCondition")) != null) {
                expression = ExpressionUtils.replaceExpression(whereExpr.toString(), var -> String.format("$\"%s\".%s#", fs[0].getTopic(), var));
            }
        }
        dto.setExpression(expression);

        Long fileId = file.getId();
        dto.setId(fileId.toString());
        dto.setDataType(file.getDataType());
        dto.setParentDataType(file.getParentDataType());
        UnsPo unsPo = origPo != null ? origPo : file;
        if (unsPo.getFields() != null) {
            FieldDefine[] fields = unsPo.getFields();
            FieldDefine[] fieldDefines = new FieldDefine[0];
            int dataType = unsPo.getDataType();
            if (dataType == Constants.TIME_SEQUENCE_TYPE || dataType == Constants.CALCULATION_REAL_TYPE) {
                LinkedList<FieldDefine> fsList = new LinkedList<>(Arrays.asList(fields));
                Iterator<FieldDefine> itr = fsList.iterator();
                CreateTopicDto dtp = UnsConverter.po2dto(unsPo);
                String tbF = dtp.getTbFieldName();
                if (tbF != null) {
                    FieldDefine dvf = dtp.getFieldDefines().getFieldsMap().get(tbF);
                    FieldDefine vf = dtp.getFieldDefines().getFieldsMap().get(Constants.SYSTEM_SEQ_VALUE);
                    vf.setName(dvf.getTbValueName());
                }
                while (itr.hasNext()) {
                    FieldDefine fd = itr.next();
                    String name = fd.getName();
                    if (name.startsWith(Constants.SYSTEM_FIELD_PREV) || fd.getTbValueName() != null) {
                        itr.remove();
                    }
                }
                fieldDefines = fsList.toArray(new FieldDefine[0]);
            } else {
                SrcJdbcType jdbcType = SrcJdbcType.getById(unsPo.getDataSrcId());
                if (jdbcType != null) {
                    FieldDefine ct = FieldUtils.getTimestampField(fields), qos = FieldUtils.getQualityField(fields, jdbcType.typeCode);
                    fieldDefines = Arrays.stream(fields)
                            .filter(fd -> fd != ct && fd != qos && !fd.getName().startsWith(Constants.SYSTEM_FIELD_PREV)).toList().toArray(new FieldDefine[0]);
                }
            }
            dto.setFields(fieldDefines);
        }
        dto.setAlias(file.getAlias());
        dto.setPath(file.getPath());
        dto.setTopic(Constants.useAliasAsTopic ? file.getAlias() : file.getPath());
        dto.setDataPath(file.getDataPath());
        String protocol = file.getProtocol();
        if (protocol != null && protocol.startsWith("{")) {
            dto.setProtocol(JsonUtil.fromJson(protocol, Map.class));
        }
        dto.setDescription(file.getDescription());
        dto.setCreateTime(getDatetime(file.getCreateAt()));
        dto.setUpdateTime(getDatetime(file.getUpdateAt()));
        dto.setAlias(file.getAlias());
        dto.setName(file.getName());
        dto.setDisplayName(file.getDisplayName());
        dto.setPathName(PathUtil.getName(file.getPath()));
        dto.setExtend(file.getExtend());
        dto.setPathType(file.getPathType());

        Integer flagsN = file.getWithFlags();
        if (flagsN != null) {
            int flags = flagsN.intValue();
            dto.setWithFlow(Constants.withFlow(flags));
            dto.setWithDashboard(Constants.withDashBoard(flags));
            dto.setWithSave2db(Constants.withSave2db(flags));
            dto.setSave2db(Constants.withSave2db(flags));
            dto.setAccessLevel(Constants.withReadOnly(flags));//北向访问级别。READ_ONLY-只读，READ_WRITE-读写
            dto.setSubscribeEnable(Constants.withSubscribeEnable(flags));
        }
        dto.setLabelList(unsLabelService.getLabelListByUnsId(fileId));

        Long templateId = file.getModelId();
        if (templateId != null) {
            UnsPo template = unsMapper.selectById(templateId);
            if (template != null) {
                dto.setModelId(templateId.toString());
                dto.setModelName(template.getName());
                dto.setTemplateName(template.getName());
                dto.setTemplateAlias(template.getAlias());
            }
        }

        dto.setMount(parseMountDetail(file, false));
        CreateTopicDto def = unsDefinitionService.getDefinitionById(id);
        if (def != null) {
            dto.setTable(def.getTable());
            dto.setTbFieldName(def.getTbFieldName());
        }
        return new JsonResult<>(0, "ok", dto);
    }

    public JsonResult<ModelDetail> getModelDefinition(Long id, String alias) {
        ModelDetail dto = new ModelDetail();
        UnsPo po;
        if (ObjectUtil.isNotNull(id)) {
            po = unsMapper.getById(id);
        } else {
            po = unsMapper.selectOne(new LambdaQueryWrapper<UnsPo>()
                    .eq(UnsPo::getPathType, Constants.PATH_TYPE_DIR)
                    .eq(UnsPo::getAlias, alias));
        }
        if (po == null) {
            return new JsonResult<>(0, I18nUtils.getMessage("uns.model.not.found"), dto);
        }

        Integer flagsN = po.getWithFlags();
        if (flagsN != null) {
            int flags = flagsN.intValue();
            dto.setSubscribeEnable(Constants.withSubscribeEnable(flags));
        }

        String protocol = po.getProtocol();
        if (dto.isSubscribeEnable() && protocol != null && protocol.startsWith("{")) {
            Map<String, String> map = JsonUtil.fromJson(protocol, Map.class);
            String frequency = map.get("frequency");
            if (frequency != null) {
                dto.setSubscribeFrequency(frequency);
            }
        }

        dto.setId(po.getId().toString());
        dto.setName(po.getName());
        dto.setDisplayName(po.getDisplayName());
        dto.setPathName(PathUtil.getName(po.getPath()));
        dto.setPath(po.getPath());
        dto.setAlias(po.getAlias());
        dto.setTopic(Constants.useAliasAsTopic ? dto.getAlias() : dto.getPath());
        dto.setDataType(po.getDataType());
        dto.setCreateTime(getDatetime(po.getCreateAt()));
        dto.setUpdateTime(getDatetime(po.getUpdateAt()));
        dto.setDescription(po.getDescription());
        dto.setParentAlias(po.getParentAlias());
        dto.setExtend(po.getExtend());
        dto.setPathType(po.getPathType());
        FieldDefine[] fs = po.getFields();
        if (fs != null) {
            for (FieldDefine f : fs) {
                f.setIndex(null);// 模型的定义 给前端消除掉 index
            }
        }
        dto.setFields(fs);

        Long templateId = po.getModelId();
        if (templateId != null) {
            UnsPo template = unsMapper.selectById(templateId);
            if (template != null) {
                dto.setModelId(templateId.toString());
                dto.setModelName(template.getName());
                dto.setTemplateAlias(template.getAlias());
            }
        }

        dto.setMount(parseMountDetail(po, false));
        return new JsonResult<>(0, "ok", dto);
    }


    private static final FieldDefineVo[] getFields(String fs) {
        if (fs != null && fs.length() > 3 && fs.charAt(0) == '[') {
            List<FieldDefineVo> list = JsonUtil.fromJson(fs, new TypeReference<List<FieldDefineVo>>() {
            }.getType());
            return list.stream().filter(f -> !f.getName().startsWith(Constants.SYSTEM_FIELD_PREV)).toArray(n -> new FieldDefineVo[n]);
        }
        return null;
    }

    private static final Long getDatetime(Date date) {
        return date != null ? date.getTime() : null;
    }

    public JsonResult<String> getLastMsgByPath(String path, boolean includeBlob) {
        CreateTopicDto def = unsDefinitionService.getDefinitionByPath(path);
        if (def != null) {
            return getLastMsg(def, includeBlob);
        } else {
            TopicMessageInfo msgInfo = externTopicLastMessages.get(path);
            if (msgInfo != null) {
                String newestMessage = JSON.toJSONString(msgInfo.jsonObject);
                return new JsonResult<>(0, "404", newestMessage);
            } else {
                return new JsonResult<>(0, "404", null);
            }
        }
    }

    public JsonResult<String> getLastMsgByAlias(String alias, boolean includeBlob) {
        CreateTopicDto def = unsDefinitionService.getDefinitionByAlias(alias);
        if (def != null) {
            return getLastMsg(def, includeBlob);
        } else {
            return new JsonResult<>(0, "404", null);
        }
    }

    public JsonResult<String> getLastMsg(Long id, boolean includeBlob) {
        CreateTopicDto def = unsDefinitionService.getDefinitionById(id);
        return getLastMsg(def, includeBlob);
    }

    private JsonResult<String> getLastMsg(CreateTopicDto def, boolean includeBlob) {
        Long id;
        if (def == null || (id = def.getId()) == null) {
            return new JsonResult<>(0, "ok", "");
        }
        TopicMessageInfo msgInfo = topicLastMessages.get(id);
        if (msgInfo == null) {
            if (def.getDataType() == Constants.CITING_TYPE && ArrayUtils.isNotEmpty(def.getRefers())) {// 引用类型
                id = def.getRefers()[0].getId();
                msgInfo = topicLastMessages.get(id);
                def = unsDefinitionService.getDefinitionById(id);
            }
        }
        if (msgInfo == null) {
            QueryLastMsgEvent event = new QueryLastMsgEvent(this, def);
            try {
                EventBus.publishEvent(event);
            } catch (Exception ex) {
                log.warn("查询最新消息失败", ex);
            }
            Map<String, Object> lastMessage = event.getLastMessage();
            if (lastMessage != null) {
                String topic = def.getTopic();
                Integer dataType = def.getDataType();
                Map<String, Long> lastDataTime = new HashMap<>(lastMessage.size());
                if (dataType == Constants.JSONB_TYPE) {
                    Object json = lastMessage.getOrDefault("json", "{}");
                    lastMessage = JSON.parseObject(json.toString(), Map.class);
                } else {
                    lastMessage.remove(Constants.SYS_SAVE_TIME);
                    Long CT = event.getMsgCreateTime();
                    for (String k : lastMessage.keySet()) {
                        lastDataTime.put(k, CT);
                    }
                    String ctField = def.getTimestampField();
                    if (ctField != null) {
                        lastMessage.replace(ctField, CT);
                    }
                }
                String rawData = JsonUtil.toJson(lastMessage);
                TopicMessageEvent topicMessageEvent = new TopicMessageEvent(
                        this, def,
                        id,
                        dataType != null ? dataType : -1,
                        def.getFieldDefines().getFieldsMap(),
                        topic,
                        def.getProtocolType(),
                        lastMessage,
                        lastMessage,
                        lastDataTime,
                        rawData,
                        event.getMsgCreateTime(),
                        null);
                onTopicMessageEvent(topicMessageEvent);
                msgInfo = topicLastMessages.get(id);
            } else {
                msgInfo = topicLastMessages.computeIfAbsent(id, k -> new TopicMessageInfo());
            }
        }
        String msg = null;
        if (msgInfo != null) {
            Integer dataType = def.getDataType();
            SerializeFilter filter = dataType != null && dataType == Constants.RELATION_TYPE ?
                    (PropertyFilter) (object, name, value) -> !Constants.SYS_FIELD_CREATE_TIME.equals(name) : null;
            msg = JSON.toJSONString(msgInfo.jsonObject, filter);
            if (includeBlob) {
                msg = DataUtils.handleBolb(msg, def);
            }
        }
        return new JsonResult<>(0, "ok", msg);
    }

    static class TopicMessageInfo {
        JSONObject jsonObject, data, dt;
        String newestMessage;
        long messageCount;
        long lastUpdateTime;

        public TopicMessageInfo() {
            this.jsonObject = new JSONObject();
        }

        synchronized void update(long lastUpdateTime, Integer dataType, String payload, Map<String, Object> data, final Map<String, Long> dt, String err) {
            jsonObject.put("updateTime", lastUpdateTime);
            jsonObject.put("msg", err);
            if (data != null) {
//                jsonObject.put("data", data);
                JSONObject dataJsonObj = jsonObject.getJSONObject("data");
                if (dataJsonObj == null) {
                    dataJsonObj = new JSONObject();
                }
                dataJsonObj.remove(Constants.SYSTEM_SEQ_TAG);
                dataJsonObj.remove(Constants.SYS_FIELD_ID);
                dataJsonObj.putAll(data);
                jsonObject.put("data", dataJsonObj);
                JSONObject dtJsonObj = jsonObject.getJSONObject("dt");
                if (dtJsonObj == null) {
                    dtJsonObj = new JSONObject();
                }
                dtJsonObj.putAll(dt);
                jsonObject.put("dt", dtJsonObj);
                jsonObject.put("payload", dataJsonObj.toJSONString());
            } else {
                jsonObject.remove("data");
                jsonObject.remove("dt");
                jsonObject.put("payload", payload);
            }

            /*JSONObject dataJsonObj = jsonObject.getJSONObject("data");
            if (dataJsonObj == null) {
                jsonObject.put("payload", payload);
            } else {
                jsonObject.put("payload", dataJsonObj.toJSONString());
            }*/
//            SerializeFilter filter = dataType != null && dataType == Constants.RELATION_TYPE ?
//                    (PropertyFilter) (object, name, value) -> !Constants.SYS_FIELD_CREATE_TIME.equals(name) : null;
//            newestMessage = JSON.toJSONString(jsonObject, filter);
            messageCount++;
            this.lastUpdateTime = lastUpdateTime;
        }

        synchronized void refresh(String payload, Map<String, Object> data) {
            if (data != null) {
                data.remove(Constants.SYSTEM_SEQ_TAG);
                jsonObject.put("data", data);
                jsonObject.put("dt", dt);
                jsonObject.put("payload", payload);
            }
            messageCount++;
            this.lastUpdateTime = lastUpdateTime;
        }
    }

    private static final ConcurrentHashMap<Long, TopicMessageInfo> topicLastMessages = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, TopicMessageInfo> externTopicLastMessages = new ConcurrentHashMap<>();

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(90)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        for (CreateTopicDto ins : event.topics) {
            topicLastMessages.remove(ins.getId());
        }
    }

    /**
     * 刷新缓存最新数据
     *
     * @param event
     */
    public void refreshLatestMsg(RefreshLatestMsgEvent event) {
        TopicMessageInfo msgInfo;
        if (event.unsId == null) {
            msgInfo = sendExternalMsg(event.path, event.payload);
            msgInfo.update(new Date().getTime(), event.dataType, event.payload, null, event.dt, null);
        } else {
            // 从缓存中取最近一条数据与最近发过来的数据进行合并
            msgInfo = topicLastMessages.computeIfAbsent(event.unsId, k -> new TopicMessageInfo());
            msgInfo.update(new Date().getTime(), event.dataType, event.payload, event.data, event.dt, "");
        }
    }

    private TopicMessageInfo sendExternalMsg(String path, String payload) {
        // 非 UNS topic
        ExternalTopicCacheDto cacheDto = new ExternalTopicCacheDto(payload, new Date());
        if (!EXTERNAL_TOPIC_CACHE.containsKey(path)) {
            EXTERNAL_TOPIC_CACHE.put(path, cacheDto);
            wssm.sendMessageBroadcastSync("refresh_notify", 2000);
        } else {
            EXTERNAL_TOPIC_CACHE.put(path, cacheDto);
        }
        return externTopicLastMessages.computeIfAbsent(path, k -> new TopicMessageInfo());
    }

    public void refreshLatestMsg(Long id) {
        CreateTopicDto def = unsDefinitionService.getDefinitionById(id);
        if (def == null) {
            return;
        }
        QueryLastMsgEvent event = new QueryLastMsgEvent(this, def);
            try {
                EventBus.publishEvent(event);
            } catch (Exception ex) {
                log.warn("查询最新消息失败", ex);
            }
            Map<String, Object> lastMessage = event.getLastMessage();
            if (lastMessage != null) {
                lastMessage.remove(Constants.SYS_SAVE_TIME);
                String payload = JsonUtil.toJson(lastMessage);
                Map<String, Long> lastDataTime = new HashMap<>(lastMessage.size());
                Long CT = event.getMsgCreateTime();
                for (String k : lastMessage.keySet()) {
                    lastDataTime.put(k, CT);
                }
                String ctField = def.getTimestampField();
                if (ctField != null) {
                    lastMessage.replace(ctField, CT);
                }

                TopicMessageInfo msgInfo = topicLastMessages.computeIfAbsent(id, k -> new TopicMessageInfo());
                msgInfo.refresh(payload, lastMessage);

                UpdateFileDTO data = new UpdateFileDTO();
                data.setAlias(def.getAlias());
                data.setData(lastMessage);
                List<UpdateFileDTO> dataList = List.of(data);
                UnsMessageEvent unsMessageEvent = new UnsMessageEvent(this, dataList);
                EventBus.publishEvent(unsMessageEvent);
            }
    }

//    @EventListener(classes = TopicMessageEvent.class)
//    @Order(9)
    void onTopicMessageEvent(TopicMessageEvent event) {
        TopicMessageInfo msgInfo;
        if (event.fieldsMap == null) {
            // 非 UNS topic
            msgInfo = sendExternalMsg(event.topic, event.payload);
            msgInfo.update(event.nowInMills, event.dataType, event.payload, null, event.lastDataTime, event.err);
            return;
        } else {
            msgInfo = topicLastMessages.computeIfAbsent(event.unsId, k -> new TopicMessageInfo());
        }
        if (!CollectionUtils.isEmpty(event.data)) {
            Map<String, Object> bean = event.lastData != null ? event.lastData : event.data;
            JSONObject data = new JSONObject(Math.max(bean.size(), 8));
            CreateTopicDto info = event.def;

            // JSONB类型文件，不需要做任何处理，直接展示原始内容
            if (info.getDataType() == Constants.JSONB_TYPE) {
                msgInfo.update(event.nowInMills, event.dataType, event.payload, event.data, event.lastDataTime, event.err);
                return;
            }

            Map<String, FieldDefine> fieldsMap = info.getFieldDefines().getFieldsMap();
            for (Map.Entry<String, Object> entry : bean.entrySet()) {
                String name = entry.getKey();
                if (name.startsWith(Constants.SYSTEM_FIELD_PREV)) {
                    continue;
                }
                Object v = entry.getValue();
                if (v != null) {
                    FieldDefine fd = fieldsMap.get(name);
                    if (fd == null) {
                        continue;
                    }
                    if (fd.isSystemField() && info.getDataType() == Constants.RELATION_TYPE) {
                        continue;
                    }
                    if (fd.getType() == FieldType.LONG || fd.getType() == FieldType.DOUBLE) {
                        v = v.toString();
                    }
                }
                data.put(name, v);
            }
            Map<String, Object> lastMsg = data;
            String tbF;
            Map<String, Long> lastDt = event.lastDataTime;

            if (info != null && (tbF = info.getTbFieldName()) != null) {
                FieldDefine tb = info.getFieldDefines().getFieldsMap().get(tbF);
                FieldDefine vf = info.getFieldDefines().getFieldsMap().get(Constants.SYSTEM_SEQ_VALUE);
                if (vf != null && tb != null) {
                    lastMsg = new LinkedHashMap<>(lastMsg);
                    lastMsg.remove(tbF);
                    Object value = lastMsg.remove(Constants.SYSTEM_SEQ_VALUE);
                    if (value != null) {
                        lastMsg.put(tb.getTbValueName(), value);
                    }
                    lastDt = new LinkedHashMap<>(lastDt);
                    lastDt.remove(tbF);
                    Object tv = lastDt.remove(Constants.SYSTEM_SEQ_VALUE);
                    if (tv instanceof Long timev) {
                        lastDt.put(tb.getTbValueName(), timev);
                    }
                }
            }
            msgInfo.update(event.nowInMills, event.dataType, event.payload, lastMsg, lastDt, event.err);
        } else {
            msgInfo.update(event.nowInMills, event.dataType, event.payload, null, event.lastDataTime, event.err);
        }
    }

//    public JsonResult<RestTestResponseVo> searchRestField(RestTestRequestVo requestVo) {
//        Triple<JsonResult<RestTestResponseVo>, FindDataListUtils.SearchResult, String> resultSearchResultPair = doSearchRestField(requestVo);
//        if (resultSearchResultPair.getLeft() != null) {
//            return resultSearchResultPair.getLeft();
//        }
//        FindDataListUtils.SearchResult rs = resultSearchResultPair.getMiddle();
//        if (CollectionUtils.isEmpty(rs.list) || !rs.dataInList) {
//            log.warn("dataListNotFound: {}, from: {}", JsonUtil.toJson(rs), resultSearchResultPair.getRight());
//            return new JsonResult<>(404, I18nUtils.getMessage("uns.rest.data404"));
//        }
//        RestTestResponseVo responseVo = new RestTestResponseVo();
//        responseVo.setDataPath(rs.dataPath);
//        responseVo.setDataFields(rs.list.get(0).keySet().stream()
//                .filter(f -> !f.startsWith(Constants.SYSTEM_FIELD_PREV)).collect(Collectors.toList()));
//        return new JsonResult<>(0, resultSearchResultPair.getRight(), responseVo);
//    }

//    private Triple<JsonResult<RestTestResponseVo>, FindDataListUtils.SearchResult, String> doSearchRestField(RestTestRequestVo requestVo) {
//        String respBody = "";
//        String url = "";
//        Object msgBody = null;
//        Map<String, Object> jsonBody = requestVo.getJsonBody();
//        if (jsonBody == null || jsonBody.isEmpty()) {
//            String[] err = new String[2];
//            url = err[1];
//            respBody = getJsonBody(requestVo, err);
//            if (err[0] != null) {
//                return Triple.of(new JsonResult<>(400, err[0] + " : " + respBody), null, respBody);
//            }
//            try {
//                msgBody = JsonUtil.fromJson(respBody);
//            } catch (Exception ex) {
//            }
//            if (msgBody == null) {
//                throw new BuzException("jsonErr: " + respBody + ", url=" + url);
//            }
//        } else {
//            msgBody = jsonBody;
//        }
//
//        FieldDefine[] fields = requestVo.getFields();
//        if (ArrayUtil.isEmpty(fields)) {
//            String topic = requestVo.getTopic();
//            if (StringUtils.hasText(topic)) {
//                JsonResult<InstanceDetail> inst = getInstanceDetail(topic);
//                InstanceDetail detail = inst.getData();
//                if (detail == null) {
//                    return Triple.of(new JsonResult<>(inst.getCode(), inst.getMsg()), null, respBody);
//                } else if (ArrayUtil.isNotEmpty(detail.getFields())) {
//                    fields = Arrays.stream(detail.getFields()).map(f -> new FieldDefine(f.getName(), FieldType.getByName(f.getType()))).toArray(n -> new FieldDefine[n]);
//                } else {
//                    return Triple.of(new JsonResult<>(400, I18nUtils.getMessage("uns.fieldsIsEmptyAt", topic)), null, respBody);//"fields is Null at topic:" + topic
//                }
//            } else {
//                return Triple.of(new JsonResult<>(400, I18nUtils.getMessage("uns.fsAndTopicIsEmpty")), null, respBody);//"both fields and topic is Null"
//            }
//        }
//        FieldDefines fieldDefines = new FieldDefines(fields);
//        FindDataListUtils.SearchResult rs = FindDataListUtils.findDataList(msgBody, 0, fieldDefines);
//        return Triple.of(null, rs, respBody);
//    }

    /**
     * 查询外部topic，并组装树状结构返回
     *
     * @param fuzzyTopic
     * @return
     */
    public List<TopicTreeResult> searchExternalTopics(String fuzzyTopic) {
        List<UnsPo> externalTopics = new ArrayList<>();
        EXTERNAL_TOPIC_CACHE.forEach((topic, date) -> {
            UnsPo uns = new UnsPo();
            uns.setId(1l);
            uns.setPath(topic);
            uns.setName(topic);
            uns.setPathType(Constants.PATH_TYPE_FILE);
            if (!StringUtils.hasText(fuzzyTopic)) {
                externalTopics.add(uns);
            } else if (topic.toLowerCase().contains(fuzzyTopic.toLowerCase())) {
                externalTopics.add(uns);
            }
        });
        return getTopicTreeResults(externalTopics, externalTopics, false);
    }

    public void clearExternalTopicCache() {
        EXTERNAL_TOPIC_CACHE.clear();
        wssm.sendMessageBroadcastSync("refresh_notify", 2000);
    }

    private List<TopicTreeResult> getTopicTreeResults(List<UnsPo> all, List<UnsPo> list, boolean showRec) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        final Map<String, TopicTreeResult> nodeMap = new HashMap<>();
        if (CollectionUtils.isEmpty(all)) {
            all = list;
        }
        for (UnsPo po : all) {
            String path = po.getPath();
            int type = po.getPathType();
            String name = po.getName();
            TopicTreeResult rs = new TopicTreeResult(name, path).setPathType(po.getPathType()).setProtocol(po.getProtocolType());
            rs.setParentDataType(po.getParentDataType());
            rs.setId(po.getId().toString());
            rs.setAlias(po.getAlias());
            if (po.getParentId() != null) {
                rs.setParentId(po.getParentId().toString());
                rs.setParentAlias(po.getParentAlias());
            }
            rs.setName(name);
            rs.setPathName(PathUtil.getName(po.getPath()));
            rs.setDisplayName(po.getDisplayName());
            rs.setExtend(po.getExtend());
            if (type == 2) {
                TopicMessageInfo info = topicLastMessages.get(path);
                if (info != null) {
                    rs.setValue(info.messageCount);
                    if (showRec) {
                        rs.setLastUpdateTime(info.lastUpdateTime);
                    }
                }
            }
            FieldDefine[] fs = po.getFields();
            if (fs != null) {
                for (FieldDefine f : fs) {
                    f.setIndex(null);// 模型的定义 给前端消除掉 index
                }
            }
            rs.setFields(fs);
            rs.setDataType(po.getDataType());
            rs.setMount(parseMountDetail(po, true));

            nodeMap.put(path, rs);
        }
        TreeMap<String, TopicTreeResult> rootNodes = new TreeMap<>();
        HashMap<String, Set<String>> childrenMap = new HashMap<>();
        for (UnsPo po : list) {
            String path = po.getPath();
            String parentPath = PathUtil.subParentPath(path);
            TopicTreeResult currentNode = nodeMap.get(path);

            // 当前节点就是根节点
            if (parentPath == null) {
                Set<String> childMap = childrenMap.computeIfAbsent(parentPath, k -> new HashSet<>());
                if (childMap.add(path)) {
                    rootNodes.put(path, currentNode);
                }
                continue;
            }

            // 当前节点不是根节点
            TopicTreeResult tempParentNode = null;
            TopicTreeResult tempCurrentNode = currentNode;
            boolean addRoot = true;
            do {
                tempParentNode = nodeMap.get(parentPath);
                if (tempParentNode == null) {
                    String name = PathUtil.getName(parentPath);
                    tempParentNode = new TopicTreeResult(name, parentPath).setPathType(0);
                    nodeMap.put(parentPath, tempParentNode);
                }
                Set<String> childMap = childrenMap.computeIfAbsent(parentPath, k -> new HashSet<>());
                if (childMap.add(tempCurrentNode.getPath())) {
                    // 节点未添加过
                    tempParentNode.addChild(tempCurrentNode);
                } else {
                    // 节点已添加过
                    addRoot = false;
                    break;
                }
                parentPath = PathUtil.subParentPath(parentPath);
                tempCurrentNode = tempParentNode;
            } while (parentPath != null);

            if (addRoot) {
                Set<String> childMap = childrenMap.computeIfAbsent(parentPath, k -> new HashSet<>());
                if (childMap.add(tempParentNode.getPath())) {
//                    if (StringUtils.hasText(tempParentNode.getId())) {
                    rootNodes.put(tempParentNode.getPath(), tempParentNode);
//                    }
                }
            }

        }
        return new ArrayList<>(rootNodes.values());
    }

    private static String getJsonBody(RestTestRequestVo requestVo, String[] err) {
        String mStr = requestVo.getMethod();
        HttpMethod method = HttpMethod.valueOf(mStr != null ? mStr.toUpperCase() : "GET");
        String url = requestVo.getFullUrl();
        if (!StringUtils.hasText(url)) {
            StringBuilder builder = new StringBuilder(256);
            RestServerConfigDTO server = requestVo.getServer();
            String host = server.getHost();
            if (!StringUtils.hasText(host)) {
                err[0] = "host is Empty";
                return null;
            }
            builder.append(host);
            String portStr = server.getPort();
            if (StringUtils.hasText(portStr)) {
                builder.append(':').append(portStr);
            }
            String uri = requestVo.getPath();
            if (StringUtils.hasText(uri)) {
                if (uri.charAt(0) != '/') {
                    builder.append('/');
                }
                builder.append(uri);
            }
            List<StrMapEntry> params = requestVo.getParams();
            PageDef pageDef = requestVo.getPageDef();
            if (!CollectionUtils.isEmpty(params) || (pageDef != null && pageDef.getStart() != null)) {
                boolean hasParams = false;
                if (params != null) {
                    Iterator<StrMapEntry> iterator = params.iterator();
                    if (iterator.hasNext()) {
                        hasParams = true;
                        builder.append('?');
                        appendKeyValueParam(builder, iterator.next());
                    }
                    while (iterator.hasNext()) {
                        builder.append('&');
                        appendKeyValueParam(builder, iterator.next());
                    }
                }
                if (pageDef != null) {
                    builder.append(hasParams ? '&' : '?');
                    StrMapEntry start = pageDef.getStart(), offset = pageDef.getOffset();
                    appendKeyValueParam(builder, start.getKey(), !"0".equals(start.getValue()) ? "1" : "0");
                    if (offset != null) {
                        builder.append('&');
                        appendKeyValueParam(builder, offset.getKey(), "1");
                    }
                }
            }
            url = builder.toString();
        }
        if (err.length > 1) {
            err[1] = url;
        }
        log.info("restUrl: {}", url);
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(method.name()), url);
        Map<String, String> headers = requestVo.getHeaderMap();
        if (headers != null) {
            // 设置headers
            request.addHeaders(headers);
        }
        String body = requestVo.getBody();
        if (StringUtils.hasText(body)) {
            request.body(body, "application/json;charset=UTF-8");
        }

        HttpResponse resp = null;
        try {
            int connTimeout = Math.max(500, requestVo.getTimeoutConnectMills()), readTimeout = Math.max(3000, requestVo.getTimeoutReadMills());
            resp = request
                    .setConnectionTimeout(connTimeout).setReadTimeout(readTimeout)
                    .setSSLSocketFactory(new DefaultSSLFactory())
                    .execute();
        } catch (Exception e) {
            log.error("call restapi error.", e);
            throw new BuzException("uns.restapi.call.error");
        }

        int status = resp.getStatus();
        if (status != 200) {
            err[0] = "status:" + status;
            return resp.body();
        }
        return resp.body();
    }

    //    public JsonResult<RestTestResponseVo> triggerRestApi(Long fileId) {
//        UnsPo file = unsMapper.selectById(fileId);
//        if (file == null) {
//            String msg = I18nUtils.getMessage("uns.file.not.exist");
//            return new JsonResult<>(0, msg);
//        }
//        final String topic = file.getAlias();
//        RestTestRequestVo requestVo = new RestTestRequestVo();
//
//        FieldDefineVo[] fs = getFields(file.getFields());
//        FieldDefine[] fieldDefines = Arrays.stream(fs).map(FieldDefineVo::convert).toArray(n -> new FieldDefine[n]);
//        requestVo.setFields(fieldDefines);
//
//        RestConfigDTO config = null;
//        if (org.apache.commons.lang3.StringUtils.isNotBlank(file.getProtocol())) {
//            config = JsonUtil.fromJson(file.getProtocol(), RestConfigDTO.class);
//        }
//        requestVo.setPath(config.getPath());
//        requestVo.setMethod(config.getMethod());
//        requestVo.setServer(config.getServer());
//        requestVo.setFullUrl(config.getFullUrl());
//
//        if (config.getPageDef() != null) {
//            requestVo.setPageDef(JsonUtil.fromJson(JsonUtil.toJson(config.getPageDef()), PageDef.class));
//        }
//
//        String body = config.getBody();
//        if (body != null && !org.apache.commons.lang3.StringUtils.equals(body, "{}")) {
//            Map<String, Object> bodyMap = JSON.parseObject(body);
//            requestVo.setBody(bodyMap);
//        }
//
//        JSONArray headers = config.getHeaders();
//        if (headers != null) {
//            List<StrMapEntry> newHeaders = new ArrayList<>();
//            for (int i = 0; i < headers.size(); i++) {
//                JSONObject headerJsonObj = headers.getJSONObject(i);
//                StrMapEntry header = new StrMapEntry();
//                header.setKey(headerJsonObj.getString("key"));
//                header.setValue(headerJsonObj.getString("value"));
//                newHeaders.add(header);
//            }
//            requestVo.setHeaders(newHeaders);
//        }
//
//        Triple<JsonResult<RestTestResponseVo>, FindDataListUtils.SearchResult, String> resultSearchResultTriple = doSearchRestField(requestVo);
//        if (resultSearchResultTriple.getLeft() != null) {
//            return resultSearchResultTriple.getLeft();
//        }
//        FindDataListUtils.SearchResult rs = resultSearchResultTriple.getMiddle();
//        if (CollectionUtils.isEmpty(rs.list) || !rs.dataInList) {
//            log.warn("dataListNotFound: {}, from: {}", JsonUtil.toJson(rs), resultSearchResultTriple.getRight());
//            return new JsonResult<>(404, I18nUtils.getMessage("uns.rest.data404"));
//        }
//
//        List<Map<String, Object>> dataList = rs.list;
//        List<FieldDefine> fieldDefineList = Arrays.stream(fieldDefines)
//                .filter(f -> !f.getName().startsWith(Constants.SYSTEM_FIELD_PREV) && f.getIndex() != null)
//                .collect(Collectors.toList());
//        List<Map<String, Object>> payload = dataList.stream().map(data -> {
//            Map<String, Object> values = new HashMap<>(data.size());
//            fieldDefineList.forEach(field -> {
//                values.put(field.getName(), data.get(field.getName()));
//            });
//            return values;
//        }).collect(Collectors.toList());
//        if (!CollectionUtils.isEmpty(payload)) {
//            try {
//                JSONObject msg = new JSONObject();
//                msg.put(Constants.MSG_RAW_DATA_KEY, JsonUtil.fromJson(resultSearchResultTriple.getRight()));
//                msg.put(Constants.MSG_RES_DATA_KEY, payload);
//
//                log.info("send message [{}] to sourceTopic[{}]", msg, topic);
//                mqttPublisher.publishMessage(topic, com.alibaba.fastjson2.JSON.toJSONBytes(msg), 0);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//        return new JsonResult<>(0, "success");
//    }
    private static final void appendKeyValueParam(StringBuilder builder, StrMapEntry entry) {
        builder.append(URLEncodeUtil.encode(entry.getKey(), StandardCharsets.UTF_8)).append('=')
                .append(URLEncodeUtil.encode(entry.getValue(), StandardCharsets.UTF_8));
    }

    private static final void appendKeyValueParam(StringBuilder builder, String key, String value) {
        builder.append(URLEncodeUtil.encode(key, StandardCharsets.UTF_8)).append('=')
                .append(URLEncodeUtil.encode(value, StandardCharsets.UTF_8));
    }

    public PageResultDTO<TemplateSearchResult> templatePageList(TemplateQueryVo params) {
        Page<UnsPo> pageParams = new Page<>(params.getPageNo(), params.getPageSize());
        IPage<TemplateSearchResult> page = unsMapper.pageListTemplates(pageParams, params);
        List<TemplateSearchResult> list = page.getRecords();
        if (CollectionUtils.isEmpty(list)) {
            return PageUtil.build(page, Collections.emptyList());
        }
        return PageUtil.build(page, list);
    }

    public TemplateVo getTemplateByAlias(String alias) {
        UnsPo po = unsMapper.getByAlias(alias);
        if (po == null) {
            return null;
        }
        return getTemplateVoByUnsPo(po);
    }

    public TemplateVo getTemplateById(Long id) {
        UnsPo po = unsMapper.selectById(id);
        if (po == null) {
            return null;
        }
        return getTemplateVoByUnsPo(po);
    }

    @NotNull
    private TemplateVo getTemplateVoByUnsPo(UnsPo po) {
        TemplateVo dto = new TemplateVo();
        dto.setId(po.getId().toString());
        dto.setAlias(po.getAlias());
        dto.setName(po.getName());
        dto.setCreateTime(getDatetime(po.getCreateAt()));
        dto.setDescription(po.getDescription());
        FieldDefine[] fs = po.getFields();
        if (fs != null) {
            for (FieldDefine f : fs) {
                f.setIndex(null);// 模型的定义 给前端消除掉 index
            }
        }
        dto.setFields(fs);

        dto.setTopic("template/" + dto.getName());

        Integer flagsN = po.getWithFlags();
        if (flagsN != null) {
            int flags = flagsN.intValue();
            dto.setSubscribeEnable(Constants.withSubscribeEnable(flags));
        }
        String protocol = po.getProtocol();
        if (dto.isSubscribeEnable() && protocol != null && protocol.startsWith("{")) {
            Map<String, String> map = JsonUtil.fromJson(protocol, Map.class);
            String frequency = map.get("frequency");
            if (frequency != null) {
                dto.setSubscribeFrequency(frequency);
            }
        }

        return dto;
    }


    public ResultVO<Integer> checkDuplicationName(String folderPath, String name, int checkType) {
        String path = name;
        long count = 0;
        if (checkType == 1) {
            // 校验文件夹名称是否已存在
            if (org.apache.commons.lang3.StringUtils.isNotBlank(folderPath)) {
                path = folderPath + name + "/";
            } else {
                path = name + "/";
            }
            count = unsMapper.selectCount(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 0).eq(UnsPo::getPath, path));
        } else if (checkType == 2) {
            // 校验文件名称是否已存在
            if (org.apache.commons.lang3.StringUtils.isNotBlank(folderPath)) {
                path = folderPath + name;
            }
            count = unsMapper.selectCount(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 2).eq(UnsPo::getPath, path));
        }

        return ResultVO.successWithData(count);
    }

    /**
     * pride 数据标准化，数字类型不要""包裹
     *
     * @param alias
     * @param data
     */
    public void standardizingData(String alias, Map<String, Object> data) {
        CreateTopicDto def = unsDefinitionService.getDefinitionByAlias(alias);
        if (def != null) {
            if (def.getDataType() == Constants.CITING_TYPE && ArrayUtils.isNotEmpty(def.getRefers())) {
                def = unsDefinitionService.getDefinitionById(def.getRefers()[0].getId());
            }
            Map<String, FieldDefine> fieldsMap = def.getFieldDefines().getFieldsMap();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                FieldDefine fd = fieldsMap.get(entry.getKey());
                if (fd != null && fd.getType().isNumber && entry.getValue() instanceof String str) {
                    BigDecimal vNum = new BigDecimal(str);
                    Object val = switch (fd.getType()) {
                        case INTEGER, LONG -> vNum.longValue();
                        case FLOAT, DOUBLE -> vNum.doubleValue();
                        default -> str;
                    };
                    data.put(entry.getKey(), val);
                }
            }
        }
    }

    public ResponseEntity<ResultVO> batchQueryFile(List<String> aliasList) {
        if (CollectionUtils.isEmpty(aliasList)) {
            return ResponseEntity.status(400).body(ResultVO.fail("别名集合为空"));
        }
        List<String> notExists = new ArrayList<>();
        JSONObject resultData = new JSONObject();
        for (String alias : aliasList) {
            CreateTopicDto dto = unsDefinitionService.getDefinitionByAlias(alias);
            if (dto == null) {
                notExists.add(alias);
                continue;
            }
            String msgInfo = getLastMsgByAlias(alias, true).getData();
            JSONObject msg = JSON.parseObject(msgInfo);
            JSONObject data;
            if (StringUtils.hasText(msgInfo) && !msg.isEmpty() && msg.getJSONObject("data") != null) {
                data = msg.getJSONObject("data");
                standardizingData(alias, data);
                if (data.containsKey(Constants.QOS_FIELD)) {
                    Object qos = data.get(Constants.QOS_FIELD);
                    if (qos != null) {
                        long v = 0;
                        if (qos instanceof Number n) {
                            v = n.longValue();
                        } else {
                            try {
                                v = Long.parseLong(qos.toString());
                            } catch (NumberFormatException ex) {
                                log.error("qos isNaN:{}, alias={}", qos, alias);
                            }
                        }
                        qos = Long.toHexString(v);
                    }
                    data.put(Constants.QOS_FIELD, qos);
                }
            } else {
                //pride：如果所订查询的文件当前无实时值，则status=通讯异常（0x80 00 00 00 00 00 00 00），timeStampe=当前服务器时间戳，value=各数据类型的初始值（详见文章节：支持数据类型）
                CreateTopicDto uns = unsDefinitionService.getDefinitionByAlias(alias);
                data = DataUtils.transEmptyValue(uns, true);
            }
            resultData.put(alias, data);
        }
        if (!CollectionUtils.isEmpty(notExists)) {
            ResultVO resultVO = new ResultVO();
            resultVO.setCode(206);
            UnsDataResponseVo res = new UnsDataResponseVo();
            res.setNotExists(notExists);
            resultVO.setData(res);
            return ResponseEntity.ok(resultVO);
        }
        return ResponseEntity.ok(ResultVO.successWithData("ok", resultData));
    }

    public List<UnsPo> listAllEmptyFolder() {
        return unsMapper.listAllEmptyFolder();
    }

    public MountDetailVo parseMountDetail(UnsPo po, boolean simple) {
        if (po.getMountType() == null || po.getMountType() == 0) {
            return null;
        }
        MountDetailVo mountDetailVo = new MountDetailVo();
        mountDetailVo.setMountType(po.getMountType());
        mountDetailVo.setMountSource(po.getMountSource());
        if (simple) {
            return mountDetailVo;
        }
        List<String> name = new ArrayList<>();
        name.add(po.getMountSource());

        String parentAlias = po.getParentAlias();
        while (parentAlias != null) {
            CreateTopicDto parent = unsDefinitionService.getDefinitionByAlias(parentAlias);
            if (parent != null) {
                if (parent.getMountType() != null && parent.getMountType() != 0) {
                    name.add(parent.getMountSource());
                    parentAlias = parent.getParentAlias();
                } else {
                    parentAlias = null;
                }
            } else {
                parentAlias = null;
            }
        }

        if (name.size() > 1) {
            Collections.reverse(name);
            mountDetailVo.setDisplayName(String.join(".", name));
        }
        return mountDetailVo;
    }

    private String fileQuerySqlDemo(Long unsId){
        StringBuilder builder = new StringBuilder("SELECT ");
        CreateTopicDto dto = unsDefinitionService.getDefinitionById(unsId);
        if (dto == null || Constants.PATH_TYPE_FILE != dto.getPathType()) {
            return null;
        }

        //0--保留（模板），1--时序，2--关系，3--计算型, 5--告警 6--聚合 7--引用
        Integer dataType = dto.getDataType();

        try {
            if (Constants.CITING_TYPE == dataType && ArrayUtils.isNotEmpty(dto.getRefers())) {
                Long sourceId = dto.getRefers()[0].getId();
                CreateTopicDto source = unsDefinitionService.getDefinitionById(sourceId);
                if (source == null) {
                    return null;
                } else {
                    dto = source;
                }
            }

            SrcJdbcType jdbcType = dataType == Constants.RELATION_TYPE ? SrcJdbcType.Postgresql : SrcJdbcType.TimeScaleDB;
            String columns = GrafanaUtils.fields2Columns(jdbcType, dto.getFields());
            String table = dto.getTable();
            String tbValue = dto.getTbFieldName();
            String tagNameCondition = "";
            int dot = table.indexOf('.');
            if (dot > 0) {
                table = table.substring(dot + 1);
            }
            builder.append(columns);
            builder.append(" FROM ").append("\\\" ");
            builder.append(table).append("\\\" ");
            if (StringUtils.hasText(tbValue)) {
                tagNameCondition = " WHERE " + Constants.SYSTEM_SEQ_TAG + "='" + dto.getId() + "' ";
                builder.append(tagNameCondition);
            }
            builder.append("LIMIT 10");
            return builder.toString();
        } catch (Exception e) {
            log.debug("", e);
            return null;
        }
    }
}
