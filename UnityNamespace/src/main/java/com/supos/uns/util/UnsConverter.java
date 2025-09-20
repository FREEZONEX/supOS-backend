package com.supos.uns.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.AlarmRuleDefine;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.ExpressionFunctions;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.PathUtil;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.TopicTreeResult;

public class UnsConverter {

    public static CreateTopicDto po2dto(UnsPo p) {
        return po2dto(p, true);
    }

    static final CopyOptions copyOptions = new CopyOptions().ignoreNullValue().ignoreError();

    public static CreateTopicDto po2dto(UnsPo p, boolean compileExpression) {
        CreateTopicDto dto = new CreateTopicDto();
        BeanUtil.copyProperties(p, dto, copyOptions);
        Integer withFlags = p.getWithFlags();
        int flags = withFlags != null ? withFlags : 0;
        dto.setFlags(withFlags);
        dto.setAddFlow(Constants.withFlow(flags));
        dto.setAddDashBoard(Constants.withDashBoard(flags));
        dto.setSave2db(Constants.withSave2db(flags));
        dto.setRetainTableWhenDeleteInstance(Constants.withRetainTableWhenDeleteInstance(flags));
        dto.setParentAlias(p.getParentAlias());
        dto.setParentId(p.getParentId());
        dto.setName(p.getName());
        dto.setLayRec(p.getLayRec());
        dto.setModelId(p.getModelId());
        dto.setProtocolType(p.getProtocolType());
        String protocolStr = p.getProtocol();
        if (protocolStr != null && protocolStr.length() > 0 && protocolStr.charAt(0) == '{') {
            JSONObject protocol = JSON.parseObject(protocolStr);
            String frequency = protocol.getString("frequency");
            if (frequency != null) {
                dto.setFrequencySeconds(getFrequencySeconds(frequency));
            }
            dto.setProtocol(protocol);
        }
        SrcJdbcType jdbcType = SrcJdbcType.getById(p.getDataSrcId());
        dto.setDataSrcId(jdbcType);
        dto.setRefers(p.getRefers());
        String calculationExpr = p.getExpression();
        dto.setExpression(calculationExpr);
        if (calculationExpr != null && !calculationExpr.isEmpty() && compileExpression) {
            dto.setCompileExpression(ExpressionFunctions.compileExpression(calculationExpr));
        }
        FieldDefine[] fields = p.getFields();
        dto.setFields(fields);
        if (dto.getDataType() != null && dto.getDataType() == Constants.ALARM_RULE_TYPE && p.getPathType() == 2) {
            AlarmRuleDefine ruleDefine = JsonUtil.fromJson(p.getProtocol(), AlarmRuleDefine.class);
            dto.setAlarmRuleDefine(ruleDefine);
        }
//        if (p.getRefUns() != null && dto.getRefFields() == null) {
//            String referId = genIdForPath(bo.getReferTopic());
//            UnsPo refPo = unsMapper.selectById(referId);
//            if (refPo == null && bo.getReferTable() != null) {
//                List<UnsPo> list = unsMapper.selectList(new QueryWrapper<UnsPo>().eq("alias", bo.getReferTable()));
//                if (list != null && !list.isEmpty()) {
//                    refPo = list.get(0);
//                }
//            }
//            if (refPo != null && refPo.getFields() != null) {
//                dto.setRefFields(JsonUtil.fromJson(refPo.getFields(), FieldDefine[].class));
//                dto.setReferModelId(refPo.getModelId());
//            }
//        }
        return dto;
    }

    private static Long getFrequencySeconds(String frequency) {
        Long nano = TimeUnits.toNanoSecond(frequency);
        if (nano != null) {
            long frequencySeconds = nano / TimeUnits.Second.toNanoSecond(1);
            return frequencySeconds;
        }
        return null;
    }

    public static TopicTreeResult dto2TreeResult(CreateTopicDto dto){
        TopicTreeResult result = new TopicTreeResult();
        result.setId(dto.getId().toString());
        result.setAlias(dto.getAlias());
        if (dto.getParentId() != null) {
            result.setParentId(String.valueOf(dto.getParentId()));
        }
        result.setParentAlias(dto.getParentAlias());
        result.setPathType(dto.getPathType());
        String name = PathUtil.getName(dto.getPath());
        result.setName(name);
        result.setPath(dto.getPath());
        result.setPathName(PathUtil.getName(dto.getPath()));
        result.setDataType(dto.getDataType());
//        result.setCountChildren(count.getCountChildren());
//        result.setHasChildren(count.getCountDirectChildren() > 0);
        return result;
    }

}
