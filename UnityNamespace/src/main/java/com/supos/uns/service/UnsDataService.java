package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.supos.common.Constants;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import com.supos.common.adpater.TopicMessageConsumer;
import com.supos.common.adpater.historyquery.*;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import com.supos.common.event.EventBus;
import com.supos.common.event.QueryDataEvent;
import com.supos.common.event.UnsMessageEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.DataUtils;
import com.supos.common.utils.DateTimeUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.util.NumberRangeValidator;
import com.supos.uns.vo.FileBlobDataQueryResult;
import com.supos.uns.vo.UnsDataResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: UnsDataService
 * @date 2025/4/15 19:42
 */
@Slf4j
@Service
public class UnsDataService {

    @Autowired
    private IUnsDefinitionService iUnsDefinitionService;
    @Autowired
    private TopicMessageConsumer topicMessageConsumer;
    @Autowired
    private UnsQueryService unsQueryService;

    private TimeSequenceDataStorageAdapter timeSequenceDataStorageAdapter;

    @EventListener(ContextRefreshedEvent.class)
    @Order
    void init(ContextRefreshedEvent event) {
        DataStorageServiceHelper storageServiceHelper = event.getApplicationContext().getBean(DataStorageServiceHelper.class);
        timeSequenceDataStorageAdapter = storageServiceHelper.getSequenceDbEnabled();
    }

    @EventListener(UnsMessageEvent.class)
    void onBatchUpdateFileEvent(UnsMessageEvent event) {
        batchUpdateFile(event.dataList);
    }

    public ResultVO<UnsDataResponseVo> batchUpdateFile(List<UpdateFileDTO> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            if (list.size() > 100) {
                return ResultVO.fail("仅支持最大一次性对100个文件进行数据更新");
            }
            List<String> aliasList = list.stream().map(UpdateFileDTO::getAlias).toList();
            //查询对应的属性
            List<String> notExists = aliasList.stream().filter(alias -> iUnsDefinitionService.getDefinitionByAlias(alias) == null).collect(Collectors.toList());
            Map<String, String> errorFields = new HashMap<>();
            for (UpdateFileDTO dto : list) {
                String alias = dto.getAlias();
                CreateTopicDto def = iUnsDefinitionService.getDefinitionByAlias(alias);
                if (def == null || def.getDataType() == null) {
                    continue;
                }
                Map<String, FieldDefine> fMap = def.getFieldDefines().getFieldsMap();
                Map<String, Object> body = dto.getData();
                if (def.getDataType() == Constants.RELATION_TYPE) {
                    for (String k : def.getFieldDefines().getUniqueKeys()) {
                        if (!k.startsWith(Constants.SYSTEM_FIELD_PREV) && !body.containsKey(k)) {
                            ResultVO<UnsDataResponseVo> resultVO = new ResultVO<>();
                            resultVO.setCode(400);
                            UnsDataResponseVo responseVo = new UnsDataResponseVo();
                            responseVo.setNotExists(notExists);
                            errorFields.put(k, I18nUtils.getMessage("uns.write.value.relation.pk.is.null"));
                            responseVo.setErrorFields(errorFields);
                            resultVO.setData(responseVo);
                            return resultVO;
                        }
                    }
                }
                JSONObject newBody = new JSONObject();
                String qosField = def.getQualityField();
                for (Map.Entry<String, Object> entry : body.entrySet()) {
                    String field = entry.getKey();
                    FieldDefine fieldDefine = fMap.get(field);
                    FieldType type = fieldDefine != null ? fieldDefine.getType() : null;
                    Object value = entry.getValue();
                    if (type != null) {
                        if (type.isNumber) {
                            if (!(value instanceof Number)) {
                                try {
                                    if (field.equals(qosField)) {
                                        value = Long.parseUnsignedLong(value.toString(), 16);//字符串的质量码都是16进制
                                    } else {
                                        Double.parseDouble(value.toString());
                                    }
                                } catch (Exception ex) {
                                    log.debug("批量写值 字段：{}，类型：{}，值：{}，字段值写入忽略：类型不匹配", field, type.name, value);
                                    errorFields.put(alias + "." + field, I18nUtils.getMessage("uns.field.type.un.match"));
                                }
                            }
                        } else if (type == FieldType.BLOB || type == FieldType.LBLOB) {
                            value = DataUtils.saveBlobData(dto.getAlias(), entry.getKey(), type, value);
                        }  else if (type == FieldType.DATETIME) {
                            value = DateTimeUtils.toUtcIso(value);
                        }
                    }
                    verifyFileField(alias, field, value, fMap, errorFields);
                    // 原样写值，消息处理内部会校验
                    newBody.put(entry.getKey(), value);
                }
                topicMessageConsumer.onMessageByAlias(dto.getAlias(), newBody.toJSONString());
            }
            if (CollectionUtils.isNotEmpty(notExists) || MapUtils.isNotEmpty(errorFields)) {
                ResultVO<UnsDataResponseVo> resultVO = new ResultVO<>();
                resultVO.setCode(206);
                UnsDataResponseVo responseVo = new UnsDataResponseVo();
                responseVo.setNotExists(notExists);
                responseVo.setErrorFields(errorFields);
                resultVO.setData(responseVo);
                return resultVO;
            }
        }
        return ResultVO.success("ok");
    }

    public ResultVO batchQueryFile(List<String> alias, List<String> paths) {
        JSONObject jsonObject = new JSONObject();
        if (!CollectionUtils.isEmpty(alias)) {
            for (String a : alias) {
                jsonObject.put(a, JsonUtil.fromJson(unsQueryService.getLastMsgByAlias(a, true).getData()));
            }
        }
        if (!CollectionUtils.isEmpty(paths)) {
            for (String pah : paths) {
                jsonObject.put(pah, JsonUtil.fromJson(unsQueryService.getLastMsgByPath(pah, true).getData()));
            }
        }
        return ResultVO.successWithData(jsonObject);
    }

    /**
     * 查询位号某时间戳的blob数据
     *
     * @param query
     * @return 返回blob数据（byte[]格式）
     */
    public FileBlobDataQueryResult queryBlobValue(FileBlobDataQueryDto query) {

        CreateTopicDto topic = iUnsDefinitionService.getDefinitionByAlias(query.getFileAlias());
        if (topic == null) {
            throw new BuzException("uns.file.not.exist");
        }

        FileBlobDataQueryResult result = new FileBlobDataQueryResult();
        List<Map<String, String>> datas = new ArrayList<>();
        result.setDatas(datas);
        boolean isBlob = false;
        FieldDefine[] fields = topic.getFields();
        for (FieldDefine field : fields) {
            if (FieldType.BLOB == field.getType()) {
                isBlob = true;
                break;
            }
        }
        if (!isBlob) {
            log.error("文件[{}]不支持blob类型", query.getFileAlias());
            return result;
        }

        // 获取到对应时间戳的值
        QueryDataEvent event = new QueryDataEvent(this, topic, query.getEqConditions());
        EventBus.publishEvent(event);

        if (!CollectionUtils.isEmpty(event.getValues())) {
            for (Map<String, Object> valueMap : event.getValues()) {
                Map<String, String> data = new HashMap<>();
                for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                    String filePath = String.format("%s/%s", Constants.BLOB_PATH, entry.getValue());
                    if (FileUtil.exist(filePath)) {
                        String value = Base64.encode(FileUtil.readBytes(filePath));
                        data.put(entry.getKey(), value);
                    }

                }
                datas.add(data);
            }

        }
        return result;
    }

    private void verifyFileField(String alias, String fieldName, Object value, Map<String, FieldDefine> fieldsMap, Map<String, String> errorFields) {
        FieldDefine fieldDefine = fieldsMap.get(fieldName);
        if (fieldDefine == null) {
            errorFields.put(alias + "." + fieldName, I18nUtils.getMessage("uns.field.not.found"));
            return;
        }

        FieldType type = fieldDefine.getType();
        String sValue = Convert.toStr(value);
        try {
            if (type == FieldType.DATETIME) {
                if (!DateTimeUtils.isValidTime(value)) {
                    errorFields.put(alias + "." + fieldName, I18nUtils.getMessage("uns.field.type.un.match"));
                }
            } else if (type == FieldType.STRING) {
                if (sValue.length() > fieldDefine.getMaxLen()) {
                    errorFields.put(alias + "." + fieldName, I18nUtils.getMessage("uns.field.value.out.of.size"));
                }
            } else if (type == FieldType.INTEGER) {
                if (NumberRangeValidator.isOutOfRange(value, FieldType.INTEGER)) {
                    errorFields.put(alias + "." + fieldName, I18nUtils.getMessage("uns.field.value.out.of.size"));
                } else {
                    Integer.parseInt(sValue);
                }
            } else if (type == FieldType.LONG) {
                if (NumberRangeValidator.isOutOfRange(value, FieldType.LONG)) {
                    errorFields.put(alias + "." + fieldName, I18nUtils.getMessage("uns.field.value.out.of.size"));
                } else {
                    Long.parseLong(sValue);
                }
            } else if (type == FieldType.FLOAT) {
                if (NumberRangeValidator.isOutOfRange(value, FieldType.FLOAT)) {
                    errorFields.put(alias + "." + fieldName, I18nUtils.getMessage("uns.field.value.out.of.size"));
                }  else {
                    Float.parseFloat(sValue);
                }
            } else if (type == FieldType.DOUBLE) {
                if (NumberRangeValidator.isOutOfRange(value, FieldType.DOUBLE)) {
                    errorFields.put(alias + "." + fieldName, I18nUtils.getMessage("uns.field.value.out.of.size"));
                } else {
                    Double.parseDouble(sValue);
                }
            }
        } catch (NumberFormatException e) {
            errorFields.put(alias + "." + fieldName, I18nUtils.getMessage("uns.field.type.un.match"));
        }
    }

    public ResponseEntity<ResultVO<UnsHistoryQueryResult>> batchQueryFileHistoryValue(HistoryValueRequest request) {
        log.debug(">>>>>>>>> 批量查询文件历史值请求参数：{}", JSONUtil.toJsonStr(request));
        HistoryQueryParams historyQueryParams = new HistoryQueryParams();
        String strategy = "none";
        if (request.getFill() != null) {
            strategy = request.getFill().getStrategy();
        }
        historyQueryParams.setFillStrategy(FillStrategy.getByNameIgnoreCase(strategy));
        if (request.getGroupBy() != null) {
            String time = request.getGroupBy().getTime();
            IntervalWindow iw = new IntervalWindow();
            if (time.indexOf(",") > 0) {
                iw.setInterval(time.split(",")[0]);
                iw.setOffset(time.split(",")[1]);
            } else {
                iw.setInterval(time);
            }
            historyQueryParams.setIntervalWindow(iw);
        }
        historyQueryParams.setLimit(request.getLimit());
        historyQueryParams.setOffset(request.getOffset());
        historyQueryParams.setAscOrder(StrUtil.equalsIgnoreCase(request.getOrder(), "asc"));

        List<String> selectList = request.getSelect();
        List<Select> selects = new ArrayList<>();
        Map<String, String> aliasMap = new TreeMap<>();
        if (!CollectionUtils.isEmpty(selectList)) {
            for (String selectStr : selectList) {
                Select select = trans2Select(selectStr, aliasMap);
                selects.add(select);
            }
        }
        //去重
        selects = selects.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Select::getTable).thenComparing(Select::getColumn))), ArrayList::new));
        historyQueryParams.setSelect(selects.toArray(new Select[0]));
        historyQueryParams.setWhere(trans2Where(request.getWhere()));
        log.debug(">>>>>>>>> 批量查询文件历史值转换后参数：{}", JSONUtil.toJsonStr(historyQueryParams));
        HistoryQueryResult result = null;
        try {
            result = timeSequenceDataStorageAdapter.queryHistory(historyQueryParams);
        } catch (Exception e) {
            log.error("", e);
            return ResponseEntity.status(500).body(ResultVO.fail(e.getMessage()));
        }
        log.debug(">>>>>>>>> 批量查询文件历史值 返回结果：{}", JSONUtil.toJsonStr(result));
        UnsHistoryQueryResult unsHistoryQueryResult = new UnsHistoryQueryResult();
        List<FieldsAndData> data = result.getResults();
        List<FieldsAndData> newData = data;
        if (!CollectionUtils.isEmpty(data)) {
            filterBlobLBlobs(data);
            filterQosToStr(data);

            List<String> orgTablist = selectList.stream().map(input -> {
                input = input.replaceAll("\"", "");
                int openIndex = input.indexOf("(");
                int closeIndex = input.indexOf(")");
                String tableAndColumn = input;
                if (openIndex != -1 && closeIndex != -1 && closeIndex > openIndex) {
                    // 括号中的部分 不带函数的表名
                    tableAndColumn = input.substring(openIndex + 1, closeIndex);
                }
                //如果有表名.字段名 ，截取表名
                if (tableAndColumn.contains(".")) {
                    tableAndColumn = tableAndColumn.split("\\.")[0];
                }
                return tableAndColumn;
            }).collect(Collectors.toList());
            newData = transToAlias(orgTablist, data, aliasMap);
        }
        unsHistoryQueryResult.setResults(newData);
        unsHistoryQueryResult.setNotExists(result.getNotExists());

        if (result.getCode() == 200) {
            return ResponseEntity.ok(ResultVO.successWithData("ok", unsHistoryQueryResult));
        } else {
            return ResponseEntity.status(400).body(ResultVO.fail(result.getMessage()));
        }
    }


    public void filterBlobLBlobs(List<FieldsAndData> list) {
        for (FieldsAndData data : list) {
            CreateTopicDto uns = iUnsDefinitionService.getDefinitionByAlias(data.getTable());
            if (uns != null && uns.isHasBlobField()) {
                String field = data.getFields().get(1);
                FieldDefine define = uns.getFieldDefines().getFieldsMap().get(field);
                List<Object[]> datas = data.getDatas();
                if (define != null && datas != null) {
                    if (define.getType() == FieldType.BLOB) {
                        // Pride 要求 BLOB 的值返回 base64格式，并且限制只返回前10条
                        datas = datas.subList(0, Math.min(10, datas.size()));
                        data.setDatas(datas);
                        for (Object[] vs : datas) {
                            String blobKey = vs[1] != null ? vs[1].toString() : null;
                            String blobBase64 = DataUtils.getBlobData(blobKey);
                            if (blobBase64 != null) {
                                vs[1] = blobBase64;
                            }
                        }
                    } else if (define.getType() == FieldType.LBLOB) {
                        for (Object[] vs : datas) {
                            vs[1] = null; // Pride 要求 LBLOB 的值都设置为空
                        }
                    }
                }
            }
        }
    }

    // pride 定制需求又改了，质量码要转成16进制字符串格式
    private void filterQosToStr(List<FieldsAndData> list) {
        for (FieldsAndData data : list) {
            List<Object[]> datas = data.getDatas();
            if (!CollectionUtils.isEmpty(datas)) {
                for (Object[] vs : datas) {
                    Object qos = vs[2];
                    if (qos != null) {
                        long v = 0;
                        if (qos instanceof Number n) {
                            v = n.longValue();
                        } else {
                            try {
                                v = Long.parseLong(qos.toString());
                            } catch (NumberFormatException ex) {
                            }
                        }
                        vs[2] = Long.toHexString(v);
                    }
                }
            }
        }
    }

    // pride 定制需求 查询引用类型需要把别名转换成原始文件的别名
    private List<FieldsAndData> transToAlias(List<String> selectList, List<FieldsAndData> dataList, Map<String, String> aliasMap) {

        Map<String, FieldsAndData> dataMap = dataList.stream().distinct().collect(Collectors.toMap(d -> d.getTable() + "." + d.getColumn(), Function.identity(), (v1, v2) -> v1));

        return mapFieldsInOrderAndReplaceTable(selectList, aliasMap, dataMap);
    }

    /**
     * 将原始表名顺序映射成 List<FieldsAndData>，并将 table 字段替换为原始名
     *
     * @param originTableList 原始表名列表
     * @param mappingMap      原始表名 -> 映射 key
     * @param resultMap       映射 key -> FieldsAndData 对象
     * @return 替换后的 List<FieldsAndData>
     */
    public static List<FieldsAndData> mapFieldsInOrderAndReplaceTable(
            Collection<String> originTableList,
            Map<String, String> mappingMap,
            Map<String, FieldsAndData> resultMap
    ) {

        List<FieldsAndData> dataList = new ArrayList<>();
        for (String mappedKey : mappingMap.keySet()) {
            FieldsAndData original = resultMap.get(mappedKey);
            if (original == null) {
                return null;
            }
            FieldsAndData newData = BeanUtil.copyProperties(original, FieldsAndData.class);
            newData.setTable(original.getTable());
            dataList.add(newData);
        }
        return dataList;
    }

//        return originTableList.stream()
//                .map(originKey -> {
//                    String mappedKey = mappingMap.get(originKey);
//                    if (mappedKey == null) {
//                        return null;
//                    }
//
//                    FieldsAndData original = resultMap.get(mappedKey);
//                    if (original == null) {
//                        return null;
//                    }
//                    FieldsAndData newData = BeanUtil.copyProperties(original, FieldsAndData.class);
//                    newData.setTable(originKey);
//                    return newData;
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }

    private Where trans2Where(JSONObject whereJson) {
        if (whereJson == null || whereJson.isEmpty()) {
            return null;
        }
        String firstKey = whereJson.keySet().iterator().next();
        JSONObject secondLevel = whereJson.getJSONObject(firstKey);
        String secondKey = secondLevel.keySet().iterator().next();
        Where where = new Where();
        List<WhereCondition> whereConditions = new ArrayList<>();
        JSONObject thirdLevel = secondLevel.getJSONObject(secondKey);
        for (String op : thirdLevel.keySet()) {
            WhereCondition whereCondition = new WhereCondition();
            whereCondition.setName(Constants.SYS_FIELD_CREATE_TIME);
            String value = thirdLevel.getString(op);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            whereCondition.setOp(QueryOperator.getByNameIgnoreCase(op));
            whereCondition.setValue(value);
            whereConditions.add(whereCondition);
        }
        if ("and".equalsIgnoreCase(firstKey)) {
            where.setAnd(whereConditions);
        } else {
            where.setOr(whereConditions);
        }
        return where;
    }

    private Select trans2Select(String input, Map<String, String> aliasMap) {
        //去除\号，Pride可能会存在
        input = input.replaceAll("\"", "");
        int openIndex = input.indexOf("(");
        int closeIndex = input.indexOf(")");
        Select select = new Select();
        String tableAndColumn = input;
        if (openIndex != -1 && closeIndex != -1 && closeIndex > openIndex) {
            // 括号前的部分
            String beforeBracket = input.substring(0, openIndex);
            //mean 函数 转avg
            if ("mean".equalsIgnoreCase(beforeBracket)) {
                beforeBracket = "avg";
            }
            select.setFunction(SelectFunction.getByNameIgnoreCase(beforeBracket));
            // 括号中的部分 first(\"SBBBBBBBBBB\")"]
            tableAndColumn = input.substring(openIndex + 1, closeIndex);
        }

        // 文件别名.字段名   or  文件别名  出现字段名则设置，无则默认为value（pride要求）
        if (tableAndColumn.contains(".")) {
            String[] tableAndColumnArray = tableAndColumn.split("\\.");
            String table = tableAndColumnArray[0];
            String column = tableAndColumnArray[1];
            select.setTable(table);
            select.setColumn(column);
        } else {
            select.setTable(tableAndColumn);
            select.setColumn("value");
        }

        String table = select.getTable();//别名
        CreateTopicDto originUns = iUnsDefinitionService.getDefinitionByAlias(table);
        //如果文件是引用类型，则需查询被引用的原始表名，并且替换
        if (originUns != null && Constants.CITING_TYPE == originUns.getDataType() && ArrayUtil.isNotEmpty(originUns.getRefers())) {
            CreateTopicDto refUns = iUnsDefinitionService.getDefinitionById(originUns.getRefers()[0].getId());
            select.setTable(refUns.getAlias());
            aliasMap.put(input, refUns.getAlias());
        } else {
            aliasMap.put(input, table);
        }
        return select;
    }

}