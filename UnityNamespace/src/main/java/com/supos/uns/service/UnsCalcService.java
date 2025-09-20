package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.StreamWindowOptionsStateWindow;
import com.supos.common.enums.FieldType;
import com.supos.common.utils.*;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsPo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsCalcService extends ServiceImpl<UnsMapper, UnsPo> {

    @Autowired
    UnsMapper baseMapper;

    static final Set<String> streamAggregateFunctions = new HashSet<>(Arrays.asList("count", "avg", "sum", "max", "min"));

    public static String checkRefers(CreateTopicDto ins) {
        if (!Integer.valueOf(Constants.PATH_TYPE_FILE).equals(ins.getPathType())) {
            return null;
        }
        InstanceField[] refers = ins.getRefers();
        Integer dataType = ins.getDataType();
        final int DATA_TYPE = dataType != null ? dataType : 0;
        if (DATA_TYPE == Constants.MERGE_TYPE) { // dataType = 7 可以支持不选择引用
            if (ArrayUtil.isEmpty(ins.getReferIds()) && ArrayUtil.isEmpty(refers)) {
                return I18nUtils.getMessage("uns.invalid.stream.empty.referTopic");
            }
            if (ins.getFrequencySeconds() == null) {
                return I18nUtils.getMessage("uns.create.empty.frequency");
            }
        }
        if (ArrayUtil.isEmpty(refers) && ArrayUtil.isNotEmpty(ins.getReferIds())) {
            Long[] refs = ins.getReferIds();
            refers = new InstanceField[refs.length];
            for (int i = 0; i < refs.length; i++) {
                refers[i] = new InstanceField(refs[i], null);
            }
            ins.setRefers(refers);
        }
        if (DATA_TYPE == Constants.CITING_TYPE && refers != null && refers.length > 1) {
            ins.setRefers(new InstanceField[]{refers[0]});// 引用类型只能引用一个，多于一个时只取第一个
        }
        if (DATA_TYPE == Constants.CALCULATION_HIST_TYPE) {
            if (ins.getStreamOptions() == null) {
                return I18nUtils.getMessage("uns.invalid.stream.window.emptyStreamOptions");
            }
            FieldDefine[] fieldDefines = ins.getFields();
            refers = new InstanceField[fieldDefines.length];
            String referTopic = ins.getReferUns();
            if (!StringUtils.hasText(referTopic)) {
                return I18nUtils.getMessage("uns.invalid.stream.empty.referTopic");
            }
            for (int i = 0, sz = refers.length; i < sz; i++) {
                FieldDefine define = fieldDefines[i];
                String fun = define.getIndex();
                int qStart = fun.indexOf('(');
                int qEnd = fun.indexOf(')');
                if (qStart > 0 && qEnd > qStart) {
                    String funName = fun.substring(0, qStart);
                    if (!streamAggregateFunctions.contains(funName.toLowerCase())) {
                        return I18nUtils.getMessage("uns.invalid.stream.func.invalid", funName);
                    }
                    String referField = fun.substring(qStart + 1, qEnd);
                    refers[i] = new InstanceField(referTopic, referField);
                } else {
                    return I18nUtils.getMessage("uns.invalid.stream.index.invalid");
                }
            }
            ins.setRefers(refers);
        }
        return null;
    }

    public static String checkExpression(CreateTopicDto inst) {
        int dataType = inst.getDataType();
        String expression = inst.getExpression();

        //当计算实例 表达式为空时 refers设置为空数组
        if (dataType == Constants.CALCULATION_REAL_TYPE && !StringUtils.hasText(expression)) {
            InstanceField[] refFields = new InstanceField[0];
            inst.setRefers(refFields);
            return null;
        }

        if (dataType == Constants.CALCULATION_REAL_TYPE || dataType == Constants.ALARM_RULE_TYPE) {
            // 校验计算实例

            if (!StringUtils.hasText(expression)) {
                String msg = I18nUtils.getMessage("uns.topic.calc.expression.empty");
                return msg;
            }
            ExpressionUtils.ParseResult rs;
            Object compileExpression;
            try {
                rs = ExpressionUtils.parseExpression(expression);
                compileExpression = ExpressionFunctions.compileExpression(expression);
            } catch (Exception ex) {
                return I18nUtils.getMessage("uns.topic.calc.expression.invalid", ex.getMessage());
            }
            for (String fun : rs.functions) {
                if (!ExpressionFunctions.hasFunction(fun)) {
                    return I18nUtils.getMessage("uns.topic.calc.expression.func.invalid", fun);
                }
            }
            InstanceField[] refFields = inst.getRefers();
            if (ArrayUtil.isEmpty(refFields)) {
                return I18nUtils.getMessage("uns.topic.calc.expression.fields.empty");
            }
            int countRefs = refFields.length;
            HashMap<String, Object> testMap = new HashMap<>();
            HashSet<Integer> indexes = new HashSet<>(Math.max(4, rs.vars.size()));
            for (String var : rs.vars) {
                String errMsg = null;
                if (var != null && var.length() > 1 && var.startsWith(Constants.VAR_PREV)) {
                    Integer refIndex = IntegerUtils.parseInt(var.substring(1));
                    if (refIndex == null) {
                        errMsg = I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.invalid", var);
                    } else if (refIndex > countRefs) {
                        errMsg = I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.indexOutOfBounds", refIndex, countRefs);
                    } else {
                        indexes.add(refIndex);
                    }
                } else {
                    errMsg = I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.invalid", var);
                }
                if (errMsg != null) {
                    return errMsg;
                }
                testMap.put(var, 1);
            }
            if (rs.vars.size() > 0 && rs.vars.size() < countRefs) {
                HashMap<String, String> replaceVar = new HashMap<>(4);
                int countEmpty = 0;
                boolean prevIsEmpty = false;
                final String preFsJson = JSON.toJSONString(refFields);
                for (int i = 1; i <= countRefs; i++) {
                    if (!indexes.contains(i)) {
                        refFields[i - 1] = null;  // 删除多余的引用
                        countEmpty++;
                        prevIsEmpty = true;
                    } else if (prevIsEmpty) {
                        replaceVar.put(Constants.VAR_PREV + i, Constants.VAR_PREV + (i - countEmpty));
                        prevIsEmpty = false;
                    }
                }
                String exp = ExpressionUtils.replaceExpression(expression, replaceVar);
                InstanceField[] newFs = new InstanceField[countRefs - countEmpty];
                for (int i = 0, k = 0; i < refFields.length; i++) {
                    if (refFields[i] != null) {
                        newFs[k++] = refFields[i];
                    }
                }
                log.info("{} 表达式优化为: {} -> {}, 字段压缩: {} -> {}", inst.getPath(), expression, exp, preFsJson, JSON.toJSONString(newFs));
                inst.setRefers(newFs);
                inst.setExpression(expression = exp);
                compileExpression = ExpressionFunctions.compileExpression(expression);
                testMap.clear();
                for (int i = 1, sz = newFs.length; i <= sz; i++) {
                    testMap.put(Constants.VAR_PREV + i, 1);
                }
            }
            try {
                Object testRs = ExpressionFunctions.executeExpression(compileExpression, testMap);
                log.debug("eval( {} ) = {}", expression, testRs);
            } catch (Exception ex) {
                return I18nUtils.getMessage("uns.topic.calc.expression.invalid", "evalErr: " + ex.getMessage());
            }
            inst.setCompileExpression(compileExpression);
        }
        return null;
    }

    public List<UnsPo> tryUpdateCalcRefUns(Map<String, String> errTipMap, Map<String, CreateTopicDto> paramInstances,
                                           Function<Long, UnsPo> allPos,
                                           Function<Long, UnsPo> dbExistsPos,
                                           Date updateAt) {
        Map<Long, Set<CreateTopicDto>> topicIdRefMap = Collections.EMPTY_MAP;
        Map<Long, CreateTopicDto> calcTopics = Collections.EMPTY_MAP;
        Iterator<Map.Entry<String, CreateTopicDto>> itr = paramInstances.entrySet().iterator();
        final int SIZE = paramInstances.size();
        while (itr.hasNext()) {
            CreateTopicDto bo = itr.next().getValue();
            Integer dataType = bo.getDataType();
            InstanceField[] refers = bo.getRefers();
            if (ArrayUtil.isEmpty(refers)) {
                if (dataType == Constants.CITING_TYPE) {
                    if (calcTopics == Collections.EMPTY_MAP) {
                        calcTopics = new HashMap<>(Math.max(16, SIZE));
                    }
                    calcTopics.put(bo.getId(), bo);
                }
                continue;
            }
            String batchIndex = bo.gainBatchIndex();
            if (topicIdRefMap == Collections.EMPTY_MAP) {
                topicIdRefMap = new HashMap<>(8);
            }
            HashMap<Long, Set<String>> refFields = new HashMap<>();
            boolean next = true;
            for (InstanceField f : refers) {
                if (f != null) {
                    Long refId = f.getId();
                    UnsPo po = allPos.apply(refId);
                    if (po == null) {
                        errTipMap.put(batchIndex, I18nUtils.getMessage("uns.topic.calc.expression.topic.ref.notFound", bo.getAlias()));
                        itr.remove();
                        next = false;
                        break;
                    }
                    Integer refDataType = po.getDataType();
                    if (refDataType != Constants.TIME_SEQUENCE_TYPE && refDataType != Constants.RELATION_TYPE && dataType.intValue() == refDataType) {
                        String t1 = I18nUtils.getMessage("uns.dataType." + dataType);
                        errTipMap.put(batchIndex, I18nUtils.getMessage("uns.ref.invalid.dataType", bo.getAlias(), t1, t1));
                        paramInstances.remove(bo.getAlias());
                        next = false;
                        break;
                    }
                    if (po.getAlias().equals(bo.getAlias())) {
                        errTipMap.put(batchIndex, I18nUtils.getMessage("uns.topic.calc.variable.itself.error"));
                        itr.remove();
                        next = false;
                        break;
                    }
                    topicIdRefMap.computeIfAbsent(refId, k -> new HashSet<>()).add(bo);
                    if (f.getField() != null) {
                        refFields.computeIfAbsent(refId, k -> new HashSet<>()).add(f.getField());
                    }
                }
            }
            if (next) {
                if (calcTopics == Collections.EMPTY_MAP) {
                    calcTopics = new HashMap<>(Math.max(16, SIZE));
                }
                calcTopics.put(bo.getId(), bo);

                bo.setRefTopicFields(refFields);
            }
        }
        if (topicIdRefMap.isEmpty() && calcTopics.isEmpty()) {
            return null;
        }
        for (Map.Entry<Long, Set<CreateTopicDto>> e : topicIdRefMap.entrySet()) {
            Long id = e.getKey();
            Set<CreateTopicDto> bos = e.getValue();
            UnsPo po = allPos.apply(id);
            String alias = po.getAlias();

            FieldDefine[] defines = po.getFields();
            Map<String, FieldDefine> fMap = Arrays.stream(defines).collect(Collectors.toMap(FieldDefine::getName, f -> f));
            final CopyOptions copyOptions = new CopyOptions().ignoreNullValue();
            for (CreateTopicDto bo : bos) {
                String batchIndex = bo.gainBatchIndex();
                if (!paramInstances.containsKey(bo.getAlias())) {
                    continue;
                }

                if (bo.getDataType() == Constants.CALCULATION_HIST_TYPE) {
                    if (bo.getStreamOptions().getWindow().getOptionBean() instanceof StreamWindowOptionsStateWindow) {
                        StreamWindowOptionsStateWindow stateWindow = (StreamWindowOptionsStateWindow) bo.getStreamOptions().getWindow().getOptionBean();
                        FieldDefine rff = fMap.get(stateWindow.getField());
                        if (rff != null) {
                            FieldType fieldType = rff.getType();
                            if (fieldType != FieldType.INTEGER && fieldType != FieldType.LONG && fieldType != FieldType.BOOLEAN && fieldType != FieldType.STRING) {
                                errTipMap.put(batchIndex, I18nUtils.getMessage("uns.invalid.stream.state.colType", stateWindow.getField(), fieldType.name));
                                paramInstances.remove(bo.getAlias());
                                continue;
                            }
                        } else {
                            errTipMap.put(batchIndex, I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.notFound", alias, stateWindow.getField()));
                            paramInstances.remove(bo.getAlias());
                            continue;
                        }
                    }
                    bo.setReferTable(po.getAlias());
                    bo.setDataPath(bo.getReferUns());
                    HashMap<String, Object> protocol = new HashMap<>();
                    BeanUtil.copyProperties(bo.getStreamOptions(), protocol, copyOptions);
                    bo.setProtocol(protocol);// 流计算的 protocol 存放 StreamOptions
                }
                Map<Long, Set<String>> refTopicFields = bo.getRefTopicFields();
                Set<String> refFs = refTopicFields != null ? refTopicFields.get(id) : null;
                if (refFs != null) {
                    for (String ref : refFs) {
                        FieldDefine rff = fMap.get(ref);
                        FieldType ft;
                        if (rff == null) {
                            errTipMap.put(batchIndex, I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.notFound", alias, ref));
                            paramInstances.remove(bo.getAlias());
                        } else if ((ft = rff.getType()) != null && (!ft.isNumber && FieldType.BOOLEAN != ft)) {
                            errTipMap.put(batchIndex, I18nUtils.getMessage(
                                    "uns.topic.calc.expression.fields.ref.invalidType", alias, ref, ft.name));
                            paramInstances.remove(bo.getAlias());
                        } else if (ft == null) {
                            errTipMap.put(batchIndex, I18nUtils.getMessage(
                                    "uns.topic.calc.expression.fields.ref.invalidType", alias, ref, "Null"));
                            paramInstances.remove(bo.getAlias());
                        }
                    }
                }
            }
        }
        HashMap<Long, Map<Long, Integer>> topicRemoveRefMap = new HashMap<>();
        HashMap<Long, Map<Long, Integer>> topicUpdateRefMap = new HashMap<>();
        for (Map.Entry<Long, CreateTopicDto> entry : calcTopics.entrySet()) {
            Long calcId = entry.getKey();
            CreateTopicDto bo = entry.getValue();
            if (!paramInstances.containsKey(bo.getAlias())) {
                continue;// 新增计算实例校验不通过的情况
            }
            UnsPo po = dbExistsPos.apply(calcId);
            InstanceField[] newRefs = bo.getRefers();
            Integer dataType = bo.getDataType();
            Set<Long> newRefTopics = Arrays.stream(newRefs).filter(Objects::nonNull).map(InstanceField::getId).collect(Collectors.toSet());
            if (po != null) {
                InstanceField[] oldRefs = ArrayUtil.isNotEmpty(po.getRefers()) ? po.getRefers() : new InstanceField[0];
                Set<Long> oldRefTopics = Arrays.stream(oldRefs).filter(Objects::nonNull).map(InstanceField::getId).collect(Collectors.toSet());
                for (Long oldRef : oldRefTopics) {
                    if (!newRefTopics.contains(oldRef)) {
                        topicRemoveRefMap.computeIfAbsent(oldRef, k -> new HashMap<>()).put(calcId, dataType);
                    }
                }
            }
            for (Long ref : newRefTopics) {
                topicUpdateRefMap.computeIfAbsent(ref, k -> new HashMap<>()).put(calcId, dataType);
            }
        }
        final int updateSize = topicRemoveRefMap.size() + topicUpdateRefMap.size();
        if (updateSize > 0) {
            List<RefUnsUpdateInfo> refUnsList = new ArrayList<>();
            addRefUns2List(topicRemoveRefMap, refUnsList);
            final int removeIndex = refUnsList.size();
            addRefUns2List(topicUpdateRefMap, refUnsList);
            this.executeBatch(refUnsList, (session, info) -> {
                UnsMapper unsMapper = session.getMapper(UnsMapper.class);
                if (info.i <= removeIndex) {
                    if (info.refIds.size() < 500) {
                        unsMapper.removeRefUns(info.id, info.refIds.keySet(), updateAt);
                    } else {
                        List<Long> ids = new ArrayList<>(info.refIds.keySet());
                        for (List<Long> refIds : Lists.partition(ids, 500)) {
                            unsMapper.removeRefUns(info.id, refIds, updateAt);
                        }
                    }
                } else {
                    if (info.refIds.size() < 200) {
                        unsMapper.updateRefUns(info.id, info.refIds, updateAt);
                    } else {
                        List<Map<Long, Integer>> rs = MapSplitter.splitMap(info.refIds, 200);
                        for (Map<Long, Integer> refIds : rs) {
                            unsMapper.updateRefUns(info.id, refIds, updateAt);
                        }
                    }
                }
            });
            ArrayList<UnsPo> updatePos = new ArrayList<>(updateSize);
            ArrayList<Long> updateIds = new ArrayList<>(updateSize);
            updateIds.addAll(topicRemoveRefMap.keySet());
            updateIds.addAll(topicUpdateRefMap.keySet());
            for (List<Long> ids : Lists.partition(updateIds, 1000)) {
                updatePos.addAll(listByIds(ids));
            }
            return updatePos;
        } else {
            return null;
        }
    }

    private static class RefUnsUpdateInfo {
        final Long id;
        final Map<Long, Integer> refIds;
        final int i;

        RefUnsUpdateInfo(Long topicId, Map<Long, Integer> refTopics, int i) {
            this.id = topicId;
            this.refIds = refTopics;
            this.i = i;
        }

    }

    private static void addRefUns2List(Map<Long, Map<Long, Integer>> refIdMap, List<RefUnsUpdateInfo> list) {
        for (Map.Entry<Long, Map<Long, Integer>> entry : refIdMap.entrySet()) {
            Long id = entry.getKey();
            list.add(new RefUnsUpdateInfo(id, entry.getValue(), list.size() + 1));
        }
    }

    public List<UnsPo> detectReferencedCalcInstance(List<UnsPo> instances, String modelPath, List<FieldDefine> delFields, boolean isDelete) {
        HashSet<Long> fileIds = new HashSet<>();
        // 存放模型关联的计算实例ID
        Set<Long> calcInstanceIds = new HashSet<>();
        instances.stream().forEach(refU -> {
            fileIds.add(refU.getId());
            Map<Long, Integer> referUns = refU.getRefUns();
            if (!CollectionUtils.isEmpty(referUns)) {
                // 提取ref_uns的topic，并根据topic计算其对应的id
                calcInstanceIds.addAll(referUns.keySet());
            }
        });
        // 批量查询引用计算实例,再根据refers字段判断删除的属性是否包含其中
        if (!calcInstanceIds.isEmpty()) {
            List<UnsPo> refersList = baseMapper.listInstancesById(calcInstanceIds);
            // 过滤出先要删除的计算实例ID
            List<UnsPo> calcUns = filterAssociatedCalcInstanceIds(delFields, fileIds, refersList);
            if (!calcUns.isEmpty()) {
                if (isDelete) {
                    List<Long> calcInstIds = calcUns.stream().map(UnsPo::getId).collect(Collectors.toList());
                    baseMapper.deleteByIds(calcInstIds);
                    log.info("--- 批量删除关联计算实例, 模型名称为： {}", modelPath);
                }
                return calcUns;
            }
        }
        return Collections.emptyList();
    }

    private List<UnsPo> filterAssociatedCalcInstanceIds(List<FieldDefine> delFields, Set<Long> fileIds, List<UnsPo> refers) {
        List<UnsPo> calcInst = new ArrayList<>();
        for (UnsPo referPO : refers) {
            InstanceField[] ref = referPO.getRefers();
            // refers数据结构为[{"field":"f1","topic":"/a/b"}]
            if (ArrayUtil.isEmpty(ref)) {
                continue;
            }
            for (InstanceField field : ref) {
                Long id = field.getId();
                String f = field.getField();
                if (id != null && f != null && fileIds.contains(id)) {
                    for (FieldDefine ff : delFields) {
                        if (ff.getName().equals(f)) {
                            calcInst.add(referPO);
                            break;
                        }
                    }
                }
            }
        }
        return calcInst;
    }
}
