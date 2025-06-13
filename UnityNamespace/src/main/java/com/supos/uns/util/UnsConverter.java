package com.supos.uns.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.AlarmRuleDefine;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.ExpressionFunctions;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsPo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UnsConverter {
    @Autowired
    UnsMapper unsMapper;

    public CreateTopicDto po2dto(UnsPo p) {
        return po2dto(p, true);
    }

    public CreateTopicDto po2dto(UnsPo p, boolean compileExpression) {
        CreateTopicDto dto = new CreateTopicDto();
        dto.setId(p.getId());
        dto.setPath(p.getPath());
        dto.setPathType(p.getPathType());
        dto.setAlias(p.getAlias());
        dto.setTableName(p.getTableName());
        dto.setDescription(p.getDescription());
        dto.setDataType(p.getDataType());
        dto.setFlags(p.getWithFlags());
        int flags = p.getWithFlags() != null ? p.getWithFlags() : 0;
        dto.setAddFlow(Constants.withFlow(flags));
        dto.setAddDashBoard(Constants.withDashBoard(flags));
        dto.setSave2db(Constants.withSave2db(flags));
        dto.setRetainTableWhenDeleteInstance(Constants.withRetainTableWhenDeleteInstance(flags));

        dto.setModelId(p.getModelId());
        dto.setProtocolType(p.getProtocolType());
        String protocolStr = p.getProtocol();
        if (protocolStr != null && protocolStr.length() > 0 && protocolStr.charAt(0) == '{') {
            JSONObject protocol = JSON.parseObject(protocolStr);
            String frequency = protocol.getString("frequency");
            if (frequency != null) {
                dto.setFrequencySeconds(getFrequencySeconds(frequency));
            }
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
        if (fields != null && fields.length > 2 && (
                (dto.getDataType() == Constants.TIME_SEQUENCE_TYPE
                        && !fields[fields.length - 1].getName().equals(Constants.SYS_SAVE_TIME))
        )
        ) {
            FieldDefine[] newFs = new FieldDefine[fields.length + 1];
            System.arraycopy(fields, 0, newFs, 0, fields.length);
            newFs[newFs.length - 1] = new FieldDefine(Constants.SYS_SAVE_TIME, FieldType.DATETIME);
            fields = newFs;// 修复末尾缺失的 _st
        }
        dto.setFields(fields);
        if (dto.getDataType() == Constants.ALARM_RULE_TYPE && p.getPathType() == 2) {
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
}
