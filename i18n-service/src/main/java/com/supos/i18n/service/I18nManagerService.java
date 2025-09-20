package com.supos.i18n.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.supos.common.exception.BuzException;
import com.supos.i18n.common.Constants;
import com.supos.i18n.common.ModuleType;
import com.supos.i18n.dao.po.I18nLanguagePO;
import com.supos.i18n.dao.po.I18nResourceModulePO;
import com.supos.i18n.dao.po.I18nVersionPO;
import com.supos.i18n.dto.AddLanguageDto;
import com.supos.i18n.dto.AddModuleDto;
import com.supos.i18n.dto.ResourceDto;
import com.supos.i18n.dto.SaveResourceBatchDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化综合管理服务
 * @date 2025/9/2 14:10
 */
@Slf4j
@Service
public class I18nManagerService {

    @Autowired
    private I18nLanguageService i18nLanguageService;

    @Autowired
    private I18nResourceModuleService i18nResourceModuleService;

    @Autowired
    private I18nVersionService i18nVersionService;

    @Autowired
    private I18nResourceService i18nResourceService;

    /**
     * 新增模块
     * @param language
     * @param throwEx
     * @return
     */
    public Map<String, String> saveLanguage(AddLanguageDto language, boolean throwEx) {
        Map<String, String> rs = i18nLanguageService.saveLanguage(language, throwEx);
        return rs;
    }

    /**
     * 删除模块（关联资源都会删掉）
     * @param languageCode
     */
    @Transactional(rollbackFor = Throwable.class)
    public void deleteLanguage(String languageCode) {
        if (Constants.BUILT_IN_LANGUAGE_CODE.contains(languageCode)) {
            throw new BuzException("i18n.exception.can_not_delete_system_language_error");
        }
        I18nLanguagePO languagePO = i18nLanguageService.getByCode(languageCode);
        if (languagePO == null) {
            return;
        }
        i18nResourceService.deleteResourceByLanguageCode(languageCode);
        i18nLanguageService.deleteLanguage(languageCode);
    }

    /**
     * 保存模块
     * @param moduleCode
     * @param moduleName
     * @param moduleType
     */
    @Transactional(rollbackFor = Throwable.class)
    public void saveModule(String moduleCode, String moduleName, ModuleType moduleType) {
        I18nResourceModulePO module = i18nResourceModuleService.getModule(moduleCode);
        if (module == null) {
            AddModuleDto moduleDto = new AddModuleDto();
            moduleDto.setModuleCode(moduleCode);
            moduleDto.setModuleName(moduleName);
            moduleDto.setModuleType(moduleType.getType());
            i18nResourceModuleService.addModule(moduleDto);
        }
    }

    /**
     * 批量保存模块
     * @param modules
     */
    @Transactional(rollbackFor = Throwable.class)
    public Map<String, String> saveModule(Collection<AddModuleDto> modules, boolean throwEx) {
        return i18nResourceModuleService.addModule(modules, throwEx);
    }

    /**
     * 删除模块（关联版本、资源都会删掉）
     * @param moduleCode
     */
    @Transactional(rollbackFor = Throwable.class)
    public void deleteModule(String moduleCode) {
        I18nResourceModulePO modulePO = i18nResourceModuleService.getModule(moduleCode);
        if (modulePO != null) {
            if (ModuleType.BUILTIN.getType() == modulePO.getModuleType()) {
                throw new BuzException("i18n.exception.can_not_delete_system_module_error");
            }

            // 删资源
            i18nResourceService.deleteResources(moduleCode);

            // 删版本
            i18nVersionService.deleteVersion(moduleCode);

            // 删模块
            i18nResourceModuleService.deleteModule(moduleCode);
        }
    }

    public String getVersion(String moduleCode) {
        I18nVersionPO i18nVersionPO = i18nVersionService.getVersion(moduleCode);
        return i18nVersionPO != null ? i18nVersionPO.getModuleVersion() : null;
    }

    public void saveVersion(String moduleCode, String moduleVersion) {
        i18nVersionService.saveVersion(moduleCode, moduleVersion);
    }

    /**
     * 批量保存i18n资源
     * @param moduleCode
     * @param languageCode
     * @param resourceFile
     */
    @Transactional(rollbackFor = Throwable.class)
    public void saveResource(String moduleCode, String languageCode, Properties resourceFile, boolean  force, boolean throwEx) {
        SaveResourceBatchDto resourceBatchDto = new SaveResourceBatchDto();

        AtomicInteger index = new AtomicInteger(0);
        List<ResourceDto> resourceDtos = new ArrayList<>(resourceFile.size());
        for (Map.Entry<Object, Object> e : resourceFile.entrySet()) {
            Integer i = index.getAndIncrement();
            // 收集国际化资源
            if (StringUtils.isBlank(e.getKey().toString())) {
                continue;
            }
            ResourceDto resourceDto = new ResourceDto();
            resourceDto.setI18nKey(e.getKey().toString());
            resourceDto.setI18nValue(e.getValue().toString());
            resourceDto.setFlagNo(String.format("0-%d",  i));
            resourceDtos.add(resourceDto);
        }
        resourceBatchDto.setModuleCode(moduleCode);
        resourceBatchDto.setLanguageCode(languageCode);
        resourceBatchDto.setResources(resourceDtos);
        i18nResourceService.saveResourceBatch(resourceBatchDto, force, throwEx);
    }

    /**
     * 批量保存i18n资源
     * @param moduleCode
     * @param languageCode
     * @param resources
     */
    @Transactional(rollbackFor = Throwable.class)
    public Map<String, String> saveResource(String moduleCode, String languageCode, List<ResourceDto> resources, boolean  force, boolean throwEx) {
        SaveResourceBatchDto resourceBatchDto = new SaveResourceBatchDto();
        resourceBatchDto.setModuleCode(moduleCode);
        resourceBatchDto.setLanguageCode(languageCode);
        resourceBatchDto.setResources(resources);
        Map<String, String> errorMap = i18nResourceService.saveResourceBatch(resourceBatchDto, force, throwEx);
        return errorMap;
    }
}
