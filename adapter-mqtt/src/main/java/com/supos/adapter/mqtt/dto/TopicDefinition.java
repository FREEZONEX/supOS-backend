package com.supos.adapter.mqtt.dto;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.ArrayUtil;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter @Setter
public class TopicDefinition {
    FieldDefines fieldDefines;
    ConcurrentHashMap<String, Object> lastMsg;
    ConcurrentHashMap<String, Long> lastDt;
    Long lastDateTime;
    Set<Long> referCalcUns; // 被引用的计算实例
    CreateTopicDto createTopicDto;
    boolean save2db;

    public TopicDefinition(CreateTopicDto createTopicDto) {
        this.initByCreateTopicDto(createTopicDto, true);
    }

    public void addReferCalcTopic(Long id) {
        if (referCalcUns == null) {
            referCalcUns = new ConcurrentHashSet<>(4);
        }
        referCalcUns.add(id);
    }

    public void removeReferCalcTopic(Long id) {
        if (referCalcUns != null) {
            referCalcUns.remove(id);
        }
    }

    public String getTopic() {
        return createTopicDto.getTopic();
    }

    public String getTable() {
        return createTopicDto.getTable();
    }

    public SrcJdbcType getJdbcType() {
        return createTopicDto.getDataSrcId();
    }

    public int getDataType() {
        return createTopicDto.getDataType();
    }

    public AlarmRuleDefine getAlarmRuleDefine() {
        return createTopicDto.getAlarmRuleDefine();
    }

    public InstanceField[] getRefers() {
        return createTopicDto.getRefers();
    }

    public Object getCompileExpression() {
        return createTopicDto.getCompileExpression();
    }

    public void setCreateTopicDto(CreateTopicDto dto) {
        initByCreateTopicDto(dto, false);
    }

    private void initByCreateTopicDto(CreateTopicDto dto, boolean init) {
        FieldDefine[] fields = dto.getFields();
        if (fields != null && fields.length > 0) {
            Map<String, FieldDefine> fieldDefineMap = dto.getFieldDefines().getFieldsMap();
            if (lastMsg != null && !lastMsg.isEmpty()) {
                for (String k : lastMsg.keySet()) {
                    if (!fieldDefineMap.containsKey(k)) {
                        lastMsg.remove(k);
                        if (lastDt != null) {
                            lastDt.remove(k);
                        }
                    }
                }
            }
            this.fieldDefines = dto.getFieldDefines();
        } else if (init) {
            this.fieldDefines = new FieldDefines();
        } else if (createTopicDto != null) {
            dto.setFields(createTopicDto.getFields());
        }
        this.createTopicDto = dto;

        Integer flags = dto.getFlags();
        if (flags != null) {
            this.save2db = Constants.withSave2db(flags);
        } else if (init) {
            this.save2db = true;
        }
        Object expr = dto.getCompileExpression();
        if (expr != null || ArrayUtil.isNotEmpty(dto.getRefers())) {
            if (dto.getDataType() == Constants.ALARM_RULE_TYPE) {
                FieldDefine rsField = fieldDefines.getFieldsMap().get(AlarmRuleDefine.FIELD_IS_ALARM);
                if (rsField == null) {
                    log.error("告警表结构错误！");
                    rsField = new FieldDefine(AlarmRuleDefine.FIELD_IS_ALARM, FieldType.BOOLEAN);
                }
                fieldDefines.setCalcField(rsField);
            } else {
                fieldDefines.setCalcField(getCalcField(dto));
            }
        }
    }

    static FieldDefine getCalcField(CreateTopicDto dto) {
        FieldDefine[] fields = dto.getFields();
        if (fields == null) {
            return null;
        }
        FieldDefine calcField = null;
        final String ct = dto.getTimestampField(), qos = dto.getQualityField();
        for (FieldDefine cv : fields) {
            final String name = cv.getName();
            if (!name.equals(ct) && !name.equals(qos) && !name.startsWith(Constants.SYSTEM_FIELD_PREV)) {
                calcField = cv;
                break;
            }
        }
        return calcField;
    }
}
