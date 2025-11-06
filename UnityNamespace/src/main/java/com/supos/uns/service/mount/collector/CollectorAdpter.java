package com.supos.uns.service.mount.collector;

import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.mount.meta.common.CommonFileMetaDto;
import com.supos.common.dto.mount.meta.gateway.*;
import com.supos.common.enums.mount.MountMetaQueryType;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.event.EventBus;
import com.supos.common.event.mount.CollectorMountMetaQueryEvent;
import com.supos.common.event.mount.MountSourceOnlineEvent;
import com.supos.uns.dao.po.UnsMountPo;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 采集器适配器，与采集器网关服务进行交互获取相关信息
 * @date 2025/9/24 14:40
 */
public class CollectorAdpter {

    /**
     * 查询采集器在离线状态
     * @param unsMountPo
     */
    public Boolean queryOnline(UnsMountPo unsMountPo) {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<Boolean> result = new AtomicReference<>();

        Consumer<Boolean> callback = (resp) -> {
            if (resp == null) {
                success.set(false);
            }
            result.set(resp);
        };

        MountSourceOnlineEvent event = new MountSourceOnlineEvent(this, MountSourceType.COLLECTOR, unsMountPo.getSourceAlias(), callback);
        EventBus.publishEvent(event);

        if (!success.get()) {
            throw new RuntimeException("query collector online error");
        }
        return result.get();
    }

    /**
     * 从采集器网关获取最新的版本信息
     * @return
     */
    public CollectorVersionResp querycollectorMetaVersion() {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<CollectorVersionResp> result = new AtomicReference<>();

        CollectorVersionReq param = new CollectorVersionReq();

        Consumer<Object> callback = (resp) -> {
            if (resp == null) {
                success.set(false);
            }
            result.set((CollectorVersionResp) resp);
        };

        CollectorMountMetaQueryEvent event = new CollectorMountMetaQueryEvent(this, null, MountMetaQueryType.COLLECTOR_VERSION,
                param, callback);
        EventBus.publishEvent(event);

        if (!success.get()) {
            throw new RuntimeException("query collector version error");
        }

        return result.get();
    }

    /**
     * 获取采集器变更信息
     * @param lowVersion
     * @param highVersion
     * @return
     */
    public CollectorMetaChangeResp queryCollectorChange(String sourceAlias, String lowVersion, String highVersion, Boolean onlyValid) {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<CollectorMetaChangeResp> result = new AtomicReference<>();

        CollectorMetaChangeReq param = new CollectorMetaChangeReq();
        param.setGatewayAlias(sourceAlias);
        param.setLowVersion(lowVersion != null ? Long.parseLong(lowVersion) : null);
        param.setHighVersion(highVersion != null ? Long.parseLong(highVersion) : null);
        param.setOnlyValid(onlyValid);

        Consumer<Object> callback = (resp) -> {
            if (resp == null) {
                success.set(false);
            }
            result.set((CollectorMetaChangeResp) resp);
        };

        CollectorMountMetaQueryEvent event = new CollectorMountMetaQueryEvent(this, null, MountMetaQueryType.COLLECTOR,
                param, callback);
        EventBus.publishEvent(event);

        if (!success.get()) {
            throw new RuntimeException("query collector change error");
        }

        return result.get();
    }

    /**
     * 获取采集器下属设备元数据
     * @param collectorAlias
     * @param lowVersion
     * @param highVersion
     * @return
     */
    public DeviceMetaChangeResp queryDeviceChange(String collectorAlias, String lowVersion, String highVersion) {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<DeviceMetaChangeResp> result = new AtomicReference<>();

        DeviceMetaChangeReq param = new DeviceMetaChangeReq();
        param.setGatewayAlias(collectorAlias);
        param.setLowVersion(lowVersion != null ? Long.parseLong(lowVersion) : null);
        param.setHighVersion(highVersion != null ? Long.parseLong(highVersion) : null);

        Consumer<Object> callback = (resp) -> {
            if (resp == null) {
                success.set(false);
            }
            result.set((DeviceMetaChangeResp) resp);
        };

        CollectorMountMetaQueryEvent event = new CollectorMountMetaQueryEvent(this, null, MountMetaQueryType.COLLECTOR_DEVICE,
                param, callback);
        EventBus.publishEvent(event);

        if (!success.get()) {
            throw new RuntimeException("query device change error");
        }

        return result.get();
    }

    /**
     * 获取采集器下属位号元数据
     * @param sourceAlias
     * @param lowVersion
     * @param highVersion
     * @return
     */
    public TagMetaChangeResp queryTagChange(String sourceAlias, Set<String> deviceAliases, String lowVersion, String highVersion) {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<TagMetaChangeResp> result = new AtomicReference<>();

        TagMetaChangeReq param = new TagMetaChangeReq();
        param.setGatewayAlias(sourceAlias);
        param.setDeviceAliases(deviceAliases);
        param.setLowVersion(lowVersion != null ? Long.parseLong(lowVersion) : null);
        param.setHighVersion(highVersion != null ? Long.parseLong(highVersion) : null);

        Consumer<Object> callback = (resp) -> {
            if (resp == null) {
                success.set(false);
            }
            result.set((TagMetaChangeResp) resp);
        };

        CollectorMountMetaQueryEvent event = new CollectorMountMetaQueryEvent(this, null, MountMetaQueryType.COLLECTOR_TAG,
                param, callback);
        EventBus.publishEvent(event);

        if (!success.get()) {
            throw new RuntimeException("query tag error");
        }

        return result.get();
    }

    public CommonFileMetaDto tag2FileMetaDto(TagMetaDto tagMetaDto) {
        CommonFileMetaDto file = new CommonFileMetaDto();
        file.setAlias(String.format("C%s", tagMetaDto.getCode()));
        file.setName(tagMetaDto.getDisplayName());
        file.setDisplayName(tagMetaDto.getDisplayName());
        file.setDescription(tagMetaDto.getDescription());
        file.setSave2db(tagMetaDto.getStorage());
        file.setOriginalAlias(tagMetaDto.getCode());

        // 属性
        CollectorDataType dataType = CollectorDataType.getByType(tagMetaDto.getValueType());
        FieldDefine[] fields = new FieldDefine[1];
        FieldDefine field = new FieldDefine();
        field.setName("value");
        field.setType(dataType.getFieldType());
        field.setDisplayName(tagMetaDto.getDisplayName());
        // 单位
        if (tagMetaDto.getUnit() != null) {
            field.setUnit(tagMetaDto.getUnit());
        }
        // 量程
        //tagMetaDto.getRange();
        fields[0] = field;
        file.setFields(fields);
        return file;
    }
}
