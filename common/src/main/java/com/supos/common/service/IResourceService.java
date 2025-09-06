package com.supos.common.service;

import com.supos.common.dto.SaveResourceDto;

public interface IResourceService {


    /**
     * 保存资源
     * 如果资源编码存在，会做更新
     * @param saveResourceDto
     * @return id
     */
    Long saveResource(SaveResourceDto saveResourceDto);

    /**
     * 根据资源编码删除及其子资源
     * @param code 资源编码
     * @return
     */
    boolean deleteByCode(String code);
}
