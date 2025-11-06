package com.supos.uns.service.mount;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.mount.MountDto;
import com.supos.common.dto.mount.MountSourceDto;
import com.supos.common.dto.mount.meta.common.CommonMountSourceDto;
import com.supos.common.enums.FolderDataType;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.exception.BuzException;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.mapper.UnsMountExtendMapper;
import com.supos.uns.dao.mapper.UnsMountMapper;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.UnsAddService;
import com.supos.uns.service.mount.adpter.MountAdpter;
import com.supos.uns.service.mount.collector.CollectorMountAdpter;
import com.supos.uns.service.mount.kafka.KafkaAdapter;
import com.supos.uns.service.mount.kafka.KafkaMountAdpter;
import com.supos.uns.service.mount.mqtt.MqttAdpter;
import com.supos.uns.service.mount.mqtt.MqttMountAdpter;
import com.supos.uns.service.mount.rabbitmq.RabbitmqMountAdpter;
import com.supos.uns.util.UnsConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: MountService
 * @date 2025/9/18 15:24
 */
@Slf4j
@Service
public class MountService {

    @Autowired
    private UnsMapper unsMapper;

    @Autowired
    private UnsMountMapper unsMountMapper;

    @Autowired
    private UnsMountExtendMapper unsMountExtendMapper;

    @Autowired
    private UnsAddService unsAddService;

    @Autowired
    private MountCoreService mountCoreService;

    /**
     * 保存挂载信息
     *
     * @param mountDto
     */
    @Transactional(rollbackFor = Exception.class)
    public void mount(MountDto mountDto) {
        // 挂载目标是否存在
        UnsPo targetUns = unsMapper.getByAlias(mountDto.getTargetAlias());
        if (targetUns == null) {
            throw new BuzException("uns.mount.target.empty");
        }
        // 挂载目标是否已经被挂载过
        UnsMountPo existMount = unsMountMapper.selectOne(Wrappers.lambdaQuery(UnsMountPo.class).eq(UnsMountPo::getTargetAlias, mountDto.getTargetAlias()), false);
        if (existMount != null) {
            throw new BuzException("uns.mount.target.already.mount");
        }
        // 挂载目标必须是空（叶子节点）
        long count = unsMapper.selectCount(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getParentId, targetUns.getId()));
        if (count > 0) {
            throw new BuzException("uns.mount.target.need.empty");
        }
        // 判断源是否已经有挂载
        existMount = unsMountMapper.selectOne(Wrappers.lambdaQuery(UnsMountPo.class).eq(UnsMountPo::getSourceAlias, mountDto.getExtend().getSourceAlias()), false);
        if (existMount != null) {
            throw new BuzException("uns.mount.source.already.mount", existMount.getTargetAlias());
        }
        // 目标
        //MountTargetType targetType = MountTargetType.getByType(mountDto.getTargetType());
        // 源
        MountSourceType sourceType = MountSourceType.getByType(mountDto.getSourceType());

        // 保存挂载信息
        // 批次号
        MountAdpter mountAdpter = null;
        if ( sourceType == MountSourceType.COLLECTOR) {
            // 采集器挂载
            mountAdpter = new CollectorMountAdpter(mountCoreService);
        } else if ( sourceType == MountSourceType.MQTT){
            mountAdpter = new MqttMountAdpter(mountCoreService);
        } else if (sourceType == MountSourceType.KAFKA) {
            mountAdpter = new KafkaMountAdpter(mountCoreService);
        } else if (sourceType == MountSourceType.RABBITMQ) {
            mountAdpter = new RabbitmqMountAdpter(mountCoreService);
        }

        // 保存挂载信息
        MountSourceDto extend = mountDto.getExtend();
        mountAdpter.createMountInfo(targetUns, mountDto);

        // 更新挂载目标文件夹的挂载源信息
        CreateTopicDto dto = UnsConverter.po2dto(targetUns);

        dto.setMountType(sourceType.getTypeValue());
        dto.setMountSource(extend.getSourceAlias());
        log.info("update mount folder source:{}, {}", targetUns.getAlias(), extend.getSourceAlias());
        JsonResult rs = unsAddService.createCategoryModelInstance(dto);
        if (rs.getCode() != 0) {
            throw new BuzException("uns.mount.error");
        }
    }



    /**
     * 查询挂载源
     * @param sourceType
     * @return
     */
    public List<CommonMountSourceDto> queryMountSource(MountSourceType sourceType) {
        if (MountSourceType.COLLECTOR == sourceType) {
            return new CollectorMountAdpter(mountCoreService).queryMountSource();
        } else if (MountSourceType.CONNECT == sourceType) {
            List<CommonMountSourceDto> sources = new ArrayList<>();
            sources.addAll(new MqttMountAdpter(mountCoreService).queryMountSource());
            sources.addAll(new KafkaMountAdpter(mountCoreService).queryMountSource());
            return sources;
        }
        return new ArrayList<>();
    }
}
