package com.supos.i18n.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.i18n.dao.mapper.I18nVersionMapper;
import com.supos.i18n.dao.po.I18nVersionPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 模块版本
 * @date 2025/9/1 10:28
 */
@Service
public class I18nVersionService extends ServiceImpl<I18nVersionMapper, I18nVersionPO> {

    public I18nVersionPO getVersion(String moduleCode) {
        return getOne(Wrappers.lambdaQuery(I18nVersionPO.class).eq(I18nVersionPO::getModuleCode, moduleCode), false);
    }

    /**
     * 校验模块版本是否有更新
     * @param moduleCode
     * @param moduleVersion
     * @return
     */
    public boolean higherVersion(String moduleCode, String moduleVersion) {
        I18nVersionPO version = getVersion(moduleCode);
        if (version == null) {
            return true;
        }
        return version.getModuleVersion().compareTo(moduleVersion) < 0;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveVersion(String moduleCode, String moduleVersion) {
        I18nVersionPO version = getOne(Wrappers.lambdaQuery(I18nVersionPO.class).eq(I18nVersionPO::getModuleCode, moduleCode), false);
        if (version == null) {
            version = new I18nVersionPO();
            version.setModuleCode(moduleCode);
            version.setModuleVersion(moduleVersion);
        } else {
            version.setModuleVersion(moduleVersion);
        }
        saveOrUpdate(version);
    }

    /**
     * 删除版本
     * @param moduleCode
     */
    @Transactional(rollbackFor = Throwable.class)
    public void deleteVersion(String moduleCode) {
        remove(Wrappers.lambdaQuery(I18nVersionPO.class).eq(I18nVersionPO::getModuleCode, moduleCode));
    }
}
