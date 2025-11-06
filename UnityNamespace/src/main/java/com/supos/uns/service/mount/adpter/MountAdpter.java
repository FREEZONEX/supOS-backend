package com.supos.uns.service.mount.adpter;

import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.mount.MountDto;
import com.supos.common.dto.mount.meta.common.CommonMountSourceDto;
import com.supos.uns.dao.po.UnsPo;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 挂载适配器接口
 * @date 2025/9/18 15:58
 */
public interface MountAdpter {

    /**
     * 创建并保存挂载信息
     * @param targetUns
     * @param mountDto
     */
    void createMountInfo(UnsPo targetUns, MountDto mountDto);

    /**
     * 挂载处理(包括挂载在离线、元数据变更等)
     */
    void handleMount();

    /**
     * 查询挂载源
     * @return
     */
    List<CommonMountSourceDto> queryMountSource();

}
