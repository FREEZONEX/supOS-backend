package com.supos.uns.service.mount.adpter;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.mount.meta.common.CommonFileMetaDto;
import com.supos.common.dto.mount.meta.common.CommonFolderMetaDto;
import com.supos.common.dto.mount.meta.connection.ConnectionResp;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.enums.mount.MountSubSourceType;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.PathUtil;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.mount.MountCoreService;
import com.supos.uns.service.mount.MountUtils;
import com.supos.uns.vo.OuterStructureVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/10/14 11:20
 */
@Slf4j
public class MetaChangeManager {

    private MountCoreService mountCoreService;

    public MetaChangeManager(MountCoreService mountCoreService) {
        this.mountCoreService = mountCoreService;
    }

    protected MountSourceType getMountSourceType() {
        throw new UnsupportedOperationException();
    }

    protected MountSubSourceType getMountSubSourceType() {
        throw new UnsupportedOperationException();
    }

    protected ConnectionResp queryConnect(String connectName) {
        throw new UnsupportedOperationException();
    }

    protected void disconnect(JSONObject oldConfig) {
        throw new UnsupportedOperationException();
    }

    protected Pair<Boolean, JSONObject> isConnectChange(JSONObject oldConfig, ConnectionResp connectionResp) {
        throw new UnsupportedOperationException();
    }

    protected void connect(JSONObject newConfig) {
        throw new UnsupportedOperationException();
    }

    protected void handleTopic(UnsMountPo mount, String connectName) {
        throw new UnsupportedOperationException();
    }

    /**
     * 元数据变更处理
     */
    public void metaChange(UnsMountPo mount) {
        try {
            String connectName = connectionChange(mount);
            handleTopic(mount, connectName);
        } catch (Throwable throwable) {
            log.error("meta change error:{}", throwable.getMessage(), throwable);
        }
    }

    /**
     * 连接变更处理
     */
    private String connectionChange(UnsMountPo unsMountPo) {
        List<UnsMountExtendPo> unsMountExtendPos = mountCoreService.queryMountExtendInfo(getMountSubSourceType(), unsMountPo.getSourceAlias(), unsMountPo.getMountSeq());
        UnsMountExtendPo unsMountExtendPo = null;
        if (CollectionUtil.isNotEmpty(unsMountExtendPos)) {
            unsMountExtendPo = unsMountExtendPos.get(0);
        }
        if (unsMountExtendPo == null || !JSON.isValid(unsMountExtendPo.getExtend())) {
            throw new BuzException("uns.mount.invalid.info");
        }
        JSONObject oldConfig = JSON.parseObject(unsMountExtendPo.getExtend());
        String connectName = oldConfig.getString("connectName");

        ConnectionResp connectionResp = queryConnect(connectName);
        if (connectionResp == null) {
            throw new BuzException("uns.mount.query.connect.error");
        } else if (CollectionUtil.isEmpty(connectionResp.getConnections())) {
            // 连接信息为空， 删掉
            disconnect(oldConfig);
            UnsPo mountFolder = mountCoreService.queryUnsByAlias(unsMountPo.getTargetAlias());
            mountCoreService.clearFolder(mountFolder);
            mountCoreService.deleteMountBySeq(unsMountPo.getMountSeq());
        } else {
            // 查看连接是否有变化
            Pair<Boolean, JSONObject> isConnectChange = isConnectChange(oldConfig, connectionResp);
            if (isConnectChange.getLeft()) {
                disconnect(oldConfig);
                unsMountExtendPo.setExtend(JSON.toJSONString(isConnectChange.getRight()));
                mountCoreService.updateMountExtend(unsMountExtendPo);
            }

            //  连接并监听
            connect(isConnectChange.getRight());
        }
        return connectName;
    }

    /**
     * 创建topic对应的文件夹
     */
    protected String createFolderForTopic(UnsMountPo unsMountPo, String connectName, String topic) {
        MountSourceType mountSourceType = getMountSourceType();
        String parentAlias = unsMountPo.getTargetAlias();
        String path = "";
        String[] topicPaths = topic.split("/");
        for (int i = 0; i < topicPaths.length - 1; i++) {
            path += "/" + topicPaths[i];
            String alias = MountUtils.alias(mountSourceType, connectName, path);

            CreateTopicDto createTopicDto = mountCoreService.getDefinitionByAlias(alias);
            if (createTopicDto != null) {
                parentAlias = alias;
                continue;
            }
            List<CommonFolderMetaDto> folders = new ArrayList<>();
            CommonFolderMetaDto folder = new CommonFolderMetaDto();
            folder.setCode(alias);
            folder.setName(topicPaths[i]);
            folder.setDisplayName(topicPaths[i]);
            folder.setMountType(mountSourceType.getTypeValue());
            folder.setMountSource(topicPaths[i]);
            folders.add(folder);
            mountCoreService.createFolder(parentAlias,  folders);
            parentAlias = alias;
        }
        return parentAlias;
    }

    public void createFileForTopic(UnsMountPo unsMountPo, String connectName, String topic, String payload, String parentAlias) {
        MountSourceType mountSourceType = getMountSourceType();
        String alias = MountUtils.alias(mountSourceType, connectName, topic);

        // 文件已存在，直接返回
        CreateTopicDto createTopicDto = mountCoreService.getDefinitionByAlias(alias);
        if (createTopicDto != null) {
            return;
        }

        // 解析文件属性
        List<OuterStructureVo> outerStructureVos = mountCoreService.parserTopicPayload(payload);
        FieldDefine[] fields = MountUtils.topic2FileFields(topic, payload, outerStructureVos);
        if (fields == null || fields.length == 0) {
            log.warn("parse topic:[{}], field empty, skip.",  topic);
            return;
        }

        String name = PathUtil.getName(topic);

        List<CommonFileMetaDto> files = new ArrayList<>();
        CommonFileMetaDto file = new CommonFileMetaDto();
        file.setAlias(alias);
        file.setName(name);
        file.setDisplayName(name);
        file.setDescription("");
        file.setOriginalAlias(name);
        file.setFields(fields);
        files.add(file);

        mountCoreService.saveFile(mountSourceType, parentAlias, unsMountPo, files);
    }
}
