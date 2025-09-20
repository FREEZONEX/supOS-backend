package com.supos.common.service;

import com.supos.common.dto.resource.SaveResource4ExternalDto;

public interface IResourceService {


    /**
     * 外部资源保存  插件 or APP
     * 如果资源编码存在，会做更新
     * @param dto
     * @return id
     */
    Long saveByExternal(SaveResource4ExternalDto dto);

    /**
     * 根据资源编码删除及其子资源
     * @param code 资源编码
     * @return
     */
    boolean deleteByCode(String code);


    /**
     * 根据菜单来源删除资源
     * @param source 来源：平台-platform  插件编码或APP编码
     * @return
     */
    boolean deleteBySource(String source);

}
