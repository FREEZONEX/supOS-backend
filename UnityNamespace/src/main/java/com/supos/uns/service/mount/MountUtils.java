package com.supos.uns.service.mount;

import cn.hutool.core.collection.CollectionUtil;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.utils.PathUtil;
import com.supos.uns.exception.UnsupportedMountTypeException;
import com.supos.uns.service.mount.adpter.MountAdpter;
import com.supos.uns.service.mount.collector.CollectorMountAdpter;
import com.supos.uns.service.mount.kafka.KafkaMountAdpter;
import com.supos.uns.service.mount.mqtt.MqttMountAdpter;
import com.supos.uns.service.mount.rabbitmq.RabbitmqMountAdpter;
import com.supos.uns.vo.OuterStructureVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: topic和uns文件转换器
 * @date 2025/10/14 10:43
 */
@Slf4j
public class MountUtils {

    public static String alias(MountSourceType mountSourceType, String connectName, String path) {
        return PathUtil.generateMd5Alias(String.format("%s_%s/%s", mountSourceType.getType(), connectName, path));
    }

    /**
     * topic转文件字段
     *
     * @param topic
     * @param payload
     * @param outerStructureVos
     * @return
     */
    public static FieldDefine[] topic2FileFields(String topic, String payload, List<OuterStructureVo> outerStructureVos) {
        Map<String, FieldDefine> fields = new HashMap<>();
        if (outerStructureVos == null) {
            return null;
        }
        if (CollectionUtil.isEmpty(outerStructureVos)) {
            // 无法解析消息，当作string处理
            log.warn("can not parse topic:[{}], payload:[{}], handle with string",  topic, StringUtils.abbreviateMiddle(payload, "*", 100));
            fields.put("value", new FieldDefine("value", FieldType.STRING));
        } else {
            // 解析处理属性
            for (OuterStructureVo outerStructureVo : outerStructureVos) {
                List<FieldDefine> fieldDefines = outerStructureVo.getFields();
                if (CollectionUtil.isNotEmpty(fieldDefines)) {
                    for (FieldDefine fieldDefine : fieldDefines) {
                        if (fields.containsKey(fieldDefine.getName())) {
                            continue;
                        }
                        fields.put(fieldDefine.getName(), fieldDefine);
                    }
                }
            }
        }

        return fields.values().toArray(new FieldDefine[fields.size()]);
    }

    public static Pair<FieldDefine[], Map<String, Object>> topic2FileFieldsWithValue(String topic, String payload, List<OuterStructureVo> outerStructureVos) {
        Map<String, FieldDefine> fields = new HashMap<>();
        Map<String, Object> valueMap = new HashMap<>();
        if (outerStructureVos == null) {
            return Pair.of(null, null);
        }
        if (CollectionUtil.isEmpty(outerStructureVos)) {
            // 无法解析消息，当作string处理
            log.warn("can not parse topic:[{}], payload:[{}], handle with string",  topic, StringUtils.abbreviateMiddle(payload, "*", 100));
            fields.put("value", new FieldDefine("value", FieldType.STRING));
        } else {
            // 解析处理属性
            for (OuterStructureVo outerStructureVo : outerStructureVos) {
                List<FieldDefine> fieldDefines = outerStructureVo.getFields();
                if (CollectionUtil.isNotEmpty(fieldDefines)) {
                    for (FieldDefine fieldDefine : fieldDefines) {
                        if (fields.containsKey(fieldDefine.getName())) {
                            continue;
                        }
                        fields.put(fieldDefine.getName(), fieldDefine);
                        if (outerStructureVo.getValueMap() != null) {
                            valueMap.put(fieldDefine.getName(), outerStructureVo.getValueMap().get(fieldDefine.getName()));
                        }
                    }
                }
            }
        }

        return Pair.of(fields.values().toArray(new FieldDefine[fields.size()]), valueMap);
    }

    public static Class<? extends MountAdpter> getMountAdapterClass(MountSourceType sourceType) {
        switch (sourceType) {
            case COLLECTOR: return CollectorMountAdpter.class;
            case MQTT: return MqttMountAdpter.class;
            case KAFKA: return KafkaMountAdpter.class;
            case RABBITMQ: return RabbitmqMountAdpter.class;
            default: throw new UnsupportedMountTypeException(400, "unsupported mount type " + sourceType.getType());
        }
    }
}
