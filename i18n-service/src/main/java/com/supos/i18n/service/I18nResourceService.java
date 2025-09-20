package com.supos.i18n.service;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.event.EventBus;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.i18n.common.Constants;
import com.supos.i18n.common.ModifyFlag;
import com.supos.i18n.dao.mapper.I18nResourceMapper;
import com.supos.i18n.dao.po.I18nLanguagePO;
import com.supos.i18n.dao.po.I18nResourceModulePO;
import com.supos.i18n.dao.po.I18nResourcePO;
import com.supos.i18n.dto.ResourceDto;
import com.supos.i18n.dto.SaveResourceBatchDto;
import com.supos.i18n.dto.SaveResourceDto;
import com.supos.i18n.dto.I18nResourceQuery;
import com.supos.i18n.event.LanguageDeleteEvent;
import com.supos.i18n.event.ResourceRefreshEvent;
import com.supos.i18n.event.ResourceSignleRefreshEvent;
import com.supos.i18n.vo.I18nResourceVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化资源 服务
 * @date 2025/9/1 9:24
 */
@Service
public class I18nResourceService extends ServiceImpl<I18nResourceMapper, I18nResourcePO> {

    private static final int I18N_KEY_LENGTH_NUM_DEFA = 255;
    private static final int I18N_VALUE_LENGTH_NUM_DEFA = 2000;

    @Autowired
    private I18nResourceMapper i18nResourceMapper;

    @Autowired
    private I18nLanguageService i18nLanguageService;

    @Autowired
    private I18nResourceModuleService i18nResourceModuleService;

    /**
     * 获取语言的所有资源
     * @param languageCode
     * @return
     */
    public List<I18nResourcePO> getAllResource(String languageCode) {
        return list(Wrappers.lambdaQuery(I18nResourcePO.class).eq(I18nResourcePO::getLanguageCode, languageCode));
    }

    /**
     * 获取语言的模块资源
     * @param languageCode
     * @param moduleCode
     * @return
     */
    public List<I18nResourcePO> getAllResourceByModule(String languageCode, String moduleCode) {
        return list(Wrappers.lambdaQuery(I18nResourcePO.class).eq(I18nResourcePO::getLanguageCode, languageCode).eq(I18nResourcePO::getModuleCode, moduleCode));
    }

    /**
     * 获取资源
     * @param languageCode
     * @param key
     * @return
     */
    public String getResourceByKey(String languageCode, String moduleCode, String key) {
        QueryWrapper<I18nResourcePO> queryWrapper = Wrappers.query(I18nResourcePO.class);
        queryWrapper.eq("i18n_key",  key);
        queryWrapper.and(queryWrapper1 -> queryWrapper1.eq("LOWER(language_code)", languageCode.toLowerCase()));

        if (moduleCode != null && !StringUtils.equals(moduleCode, "null")) {
            queryWrapper.eq("module_code", moduleCode);
        }
        List<I18nResourcePO> i18nResourcePOs = list(queryWrapper);
        if (CollectionUtil.isNotEmpty(i18nResourcePOs)) {
            return i18nResourcePOs.get(0).getI18nValue();
        }
        return null;
    }

    public List<I18nResourcePO> getResourceByKey(String moduleCode, String key) {
        QueryWrapper<I18nResourcePO> queryWrapper = Wrappers.query(I18nResourcePO.class);
        queryWrapper.eq("i18n_key",  key);
        queryWrapper.eq("module_code", moduleCode);
        return list(queryWrapper);
    }

    public List<I18nResourcePO> getResourceByLanguageAndValue(String languageCode, String value) {
        QueryWrapper<I18nResourcePO> queryWrapper = Wrappers.query(I18nResourcePO.class);
        queryWrapper.and(queryWrapper1 -> queryWrapper1.eq("LOWER(language_code)", languageCode.toLowerCase()));
        if (value != null && value != "") {
            queryWrapper.like("i18n_value", value);
        }
        return list(queryWrapper);
    }

    /**
     * 查询国际化资源
     * @param query
     * @return
     */
    public PageResultDTO<I18nResourceVO> search(I18nResourceQuery query) {
        Page<I18nResourcePO> page = new Page<>(query.getPageNo(), query.getPageSize());

        LambdaQueryWrapper<I18nResourcePO> queryWrapper = Wrappers.lambdaQuery(I18nResourcePO.class);
        queryWrapper.select(I18nResourcePO::getI18nKey);

        if (StringUtils.isNotBlank(query.getModuleCode())) {
            queryWrapper.eq(I18nResourcePO::getModuleCode, query.getModuleCode());
        }
        if (StringUtils.isNotBlank(query.getKeyword())) {
            queryWrapper.and(queryWrapper1 ->
                            queryWrapper1.like(I18nResourcePO::getI18nValue, query.getKeyword())
                                    .or()
                                    .like(I18nResourcePO::getI18nKey, query.getKeyword()));
        }
        queryWrapper.groupBy(I18nResourcePO::getI18nKey);
        queryWrapper.orderByAsc(I18nResourcePO::getI18nKey);

        IPage<I18nResourcePO> pageResult = i18nResourceMapper.selectPage(page, queryWrapper);
        List<I18nResourceVO> list = new ArrayList<>();
        if (pageResult != null && CollectionUtil.isNotEmpty(pageResult.getRecords())) {
            Set<String> keys = pageResult.getRecords().stream().map(I18nResourcePO::getI18nKey).collect(Collectors.toSet());

            LambdaQueryWrapper<I18nResourcePO> detailQueryWrapper = Wrappers.lambdaQuery(I18nResourcePO.class);
            if (StringUtils.isNotBlank(query.getModuleCode())) {
                detailQueryWrapper.eq(I18nResourcePO::getModuleCode, query.getModuleCode());
            }
            detailQueryWrapper.in(I18nResourcePO::getI18nKey, keys);
            List<I18nResourcePO> detailList = i18nResourceMapper.selectList(detailQueryWrapper);

            Map<String, List<I18nResourcePO>> detailMap = detailList.stream().collect(Collectors.groupingBy(I18nResourcePO::getI18nKey));
            pageResult.getRecords().forEach(record -> {
                List<I18nResourcePO> resources = detailMap.get(record.getI18nKey());
                I18nResourceVO resourceVO = new I18nResourceVO();
                Map<String, String> values = new HashMap<>();

                if (CollectionUtil.isNotEmpty(resources)) {
                    resources.forEach(resource -> {
                        values.put(resource.getLanguageCode(), resource.getI18nValue());
                    });
                }
                resourceVO.setI18nKey(record.getI18nKey());
                resourceVO.setValues(values);
                list.add(resourceVO);
            });
        }

        PageResultDTO.PageResultDTOBuilder<I18nResourceVO> pageBuilder = PageResultDTO.<I18nResourceVO>builder().total(pageResult.getTotal()).pageNo(query.getPageNo()).pageSize(query.getPageSize());
        return pageBuilder.code(0).data(list).build();
    }

    /**
     * 新增国际化资源
     * @param resource
     */
    public void addResources(SaveResourceDto resource) {
        checkModuleCode(resource.getModuleCode(), true);
        checkLanguageCode(resource.getValues().keySet(), true);

        checkResourcesKey(resource.getModuleCode(), resource.getKey(), true);
        checkResourcesKeysExist(resource.getModuleCode(), List.of(resource.getKey()));

        for (String value : resource.getValues().values()) {
            checkResourcesValue(value, true);
        }

        List<I18nResourcePO> datas = new ArrayList<>();
        for (Map.Entry<String, String> e : resource.getValues().entrySet()) {
            I18nResourcePO po  = new I18nResourcePO();
            po.setModuleCode(resource.getModuleCode());
            po.setLanguageCode(e.getKey().trim());
            po.setI18nKey(resource.getKey());
            po.setI18nValue(StringUtils.isBlank(e.getValue()) ? "" : e.getValue().trim());
            po.setModifyFlag(ModifyFlag.CUSTOM.getFlag());
            datas.add(po);
        }
        saveBatch(datas);
    }

    private String checkModuleCode(String moduleCode, boolean throwEx) {
        if (StringUtils.isBlank(moduleCode)) {
            if (throwEx) {
                throw new BuzException("param.null", "moduleCode");
            }else {
                return I18nUtils.getMessage("param.null", "moduleCode");
            }
        }
        // 校验模块是否存在
        I18nResourceModulePO module = i18nResourceModuleService.getModule(moduleCode);
        if (module == null) {
            if (throwEx) {
                throw new BuzException("i18n.exception.module_code_has_no_error");
            }else {
                return I18nUtils.getMessage("i18n.exception.module_code_has_no_error");
            }
        }
        return null;
    }

    private String checkLanguageCode(Collection<String> languageCodes, boolean throwEx) {
        if (CollectionUtil.isEmpty(languageCodes)) {
            if (throwEx) {
                throw new BuzException("i18n.exception.param_lost_i18n_language");
            } else {
                return I18nUtils.getMessage("i18n.exception.param_lost_i18n_language");
            }
        }

        Set<String> allLanguageCodes = i18nLanguageService.getAllLanguageCode(false);
        for (String languageCode : languageCodes) {
            // language是否为空
            if (StringUtils.isBlank(languageCode.trim())) {
                if (throwEx) {
                    throw new BuzException("i18n.exception.param_lost_i18n_language");
                } else {
                    return I18nUtils.getMessage("i18n.exception.param_lost_i18n_language");
                }
            }
            // language是否存在
            if (!allLanguageCodes.contains(languageCode.trim())) {
                if (throwEx) {
                    throw new BuzException("i18n.exception.language_has_no_error");
                } else {
                    return I18nUtils.getMessage("i18n.exception.language_has_no_error");
                }
            }
        }
        return null;
    }

    /**
     * 校验资源健
     *
     * @param key
     */
    private String checkResourcesKey(String moduleCode, String key, boolean throwEx) {
        if (StringUtils.isBlank(key)) {
            if (throwEx) {
                throw new BuzException("param.null", "i18nKey");
            } else {
                return I18nUtils.getMessage("param.null", "i18nKey");
            }
        }
        // 是否超长
        if (key.trim().length() > I18N_KEY_LENGTH_NUM_DEFA) {
            if (throwEx) {
                throw new BuzException("i18n.exception.length_limit_error", "i18nKey", I18N_KEY_LENGTH_NUM_DEFA);
            } else {
                return I18nUtils.getMessage("i18n.exception.length_limit_error", "i18nKey", I18N_KEY_LENGTH_NUM_DEFA);
            }
        }
/*        if (!moduleCode.equals(Constants.DEFAULT_MODULE_CODE)) {
            if (!key.startsWith(moduleCode)) {
                if (throwEx) {
                    throw new BuzException("i18n.exception.i18n_key_start_error");
                } else {
                    return I18nUtils.getMessage("i18n.exception.i18n_key_start_error");
                }
            }
        }*/

        return null;
    }

    private void checkResourcesKeysExist(String moduleCode, List<String> keys) {
        // 是否已存在
        List<List<String>> spKeys = CollectionUtil.split(keys, 10);
        for (List<String> ks : spKeys) {
            List<Object> is = i18nResourceMapper.selectObjs(new QueryWrapper<I18nResourcePO>().lambda()
                    .select(I18nResourcePO::getI18nKey)
                    .eq(I18nResourcePO::getModuleCode, moduleCode)
                    .in(I18nResourcePO::getI18nKey, ks.toArray()));
            if (!CollectionUtil.isEmpty(is)) {
                throw new BuzException("i18n.exception.i18n_key_exist");
            }
        }
    }

    /**
     * 校验资源值
     *
     * @param value
     */
    private String checkResourcesValue(String value, boolean throwEx) {
        // value是否为空
/*        if (org.springframework.util.StringUtils.isEmpty(value.trim())) {
            throw new BuzException("i18n.exception.param_lost_i18n_value");
        }*/
        if (StringUtils.isBlank(value)) {
            return null;
        }
        // value是否超长
        if (value.trim().length() > I18N_VALUE_LENGTH_NUM_DEFA) {
            if (throwEx) {
                throw new BuzException("i18n.exception.length_limit_error", "i18nValue", I18N_VALUE_LENGTH_NUM_DEFA);
            } else {
                return I18nUtils.getMessage("i18n.exception.length_limit_error", "i18nValue", I18N_VALUE_LENGTH_NUM_DEFA);
            }
        }
        return null;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateResources(SaveResourceDto resource) {
        deleteResources(resource.getModuleCode(), resource.getKey());
        addResources(resource);
        EventBus.publishEvent(new ResourceSignleRefreshEvent(this, resource.getKey()));
    }

    /**
     * 批量保存国际化资源
     * @param resourceBatchDto
     * @param force 是否强制更新
     */
    @Transactional(rollbackFor = Throwable.class)
    public Map<String, String> saveResourceBatch(SaveResourceBatchDto resourceBatchDto, boolean force, boolean throwEx) {
        Map<String, String> errorMap = new HashMap<>();
        String moduleCode = resourceBatchDto.getModuleCode();
        String languageCode = resourceBatchDto.getLanguageCode();

        String checkModuleCodeError = checkModuleCode(moduleCode, throwEx);
        // 校验语言是否存在
        String checkLanguageCodeError = checkLanguageCode(List.of(languageCode), throwEx);

        // 清空模块资源
        if (checkModuleCodeError == null && checkLanguageCodeError == null) {
            if (CollectionUtil.isEmpty(resourceBatchDto.getResources())) {
                // 直接清空模块资源
                remove(Wrappers.lambdaQuery(I18nResourcePO.class).eq(I18nResourcePO::getModuleCode, moduleCode)
                        .eq(I18nResourcePO::getLanguageCode, languageCode));
                EventBus.publishEvent(new ResourceRefreshEvent(this, languageCode));
                return errorMap;
            }
        }

        // 更新操作
        String error = null;
        // 国际化Key
        List<ResourceDto> resources = new ArrayList<>(resourceBatchDto.getResources() != null ? resourceBatchDto.getResources().size() : 0);
        for (ResourceDto resource : resourceBatchDto.getResources()) {
            if (checkModuleCodeError != null) {
                errorMap.put(resource.getFlagNo(), checkModuleCodeError);
                continue;
            }

            if (checkLanguageCodeError != null) {
                errorMap.put(resource.getFlagNo(), checkLanguageCodeError);
                continue;
            }

            error = checkResourcesKey(moduleCode, resource.getI18nKey(), throwEx);
            if (error != null) {
                errorMap.put(resource.getFlagNo(), error);
                continue;
            }

            error = checkResourcesValue(resource.getI18nValue(), throwEx);
            if (error != null) {
                errorMap.put(resource.getFlagNo(), error);
                continue;
            }
            resources.add(resource);
        }

        Map<String, ResourceDto> newResourceMap = resources.stream().collect(Collectors.toMap(ResourceDto::getI18nKey, Function.identity(), (k1, k2) -> k2));

        List<I18nResourcePO> existResources = list(Wrappers.lambdaQuery(I18nResourcePO.class)
                .eq(I18nResourcePO::getLanguageCode, languageCode).eq(I18nResourcePO::getModuleCode, moduleCode));
        Map<String, I18nResourcePO> existResourceMap = existResources.stream().collect(Collectors.toMap(I18nResourcePO::getI18nKey, Function.identity(), (k1, k2) -> k2));

        List<I18nResourcePO> addResourceList = new ArrayList<>();
        List<I18nResourcePO> updateResourceList = new ArrayList<>();
        for (Map.Entry<String, ResourceDto> entry : newResourceMap.entrySet()) {
            ResourceDto resourceDto = entry.getValue();
            I18nResourcePO existResource = existResourceMap.get(entry.getKey());
            if (existResource == null) {
                // 新增
                I18nResourcePO resourcePO  = new I18nResourcePO();
                resourcePO.setModuleCode(moduleCode);
                resourcePO.setLanguageCode(languageCode);
                resourcePO.setI18nKey(resourceDto.getI18nKey().trim());
                resourcePO.setI18nValue(StringUtils.isBlank(resourceDto.getI18nValue()) ? "" : resourceDto.getI18nValue().trim());

                if (force) {
                    resourcePO.setModifyFlag(ModifyFlag.CUSTOM.getFlag());
                } else {
                    resourcePO.setModifyFlag(ModifyFlag.SYSTEM.getFlag());
                }
                addResourceList.add(resourcePO);
            } else {
                // 更新
                if (force || (existResource.getModifyFlag() == null || ModifyFlag.SYSTEM.getFlag().equals(existResource.getModifyFlag()))) {
                    if (!StringUtils.equals(resourceDto.getI18nValue(), existResource.getI18nValue())) {
                        existResource.setI18nValue(StringUtils.isBlank(resourceDto.getI18nValue()) ? "" : resourceDto.getI18nValue().trim());
                        if (force) {
                            existResource.setModifyFlag(ModifyFlag.CUSTOM.getFlag());
                        } else {
                            existResource.setModifyFlag(ModifyFlag.SYSTEM.getFlag());
                        }
                        existResource.setModifyAt(new Date());
                        updateResourceList.add(existResource);
                    }
                }
            }
        }

        // 删除
        List<Long> deleteResourceList = new ArrayList<>();
        for (Map.Entry<String, I18nResourcePO> entry : existResourceMap.entrySet()) {
            I18nResourcePO existResource = entry.getValue();
            if (!newResourceMap.containsKey(entry.getKey())) {
                if (force || (existResource.getModifyFlag() == null || ModifyFlag.SYSTEM.getFlag().equals(existResource.getModifyFlag()))) {
                    deleteResourceList.add(existResource.getId());
                }
            }
        }

        if (CollectionUtil.isNotEmpty(addResourceList)) {
            saveBatch(addResourceList);
        }
        if (CollectionUtil.isNotEmpty(updateResourceList)) {
            updateBatchById(updateResourceList);
        }
        if (CollectionUtil.isNotEmpty(deleteResourceList)) {
            removeByIds(deleteResourceList);
        }
        EventBus.publishEvent(new ResourceRefreshEvent(this, languageCode));
        return errorMap;
    }

    /**
     * 删除资源
     * @param moduleCode
     * @param key
     */
    @Transactional(rollbackFor = Throwable.class)
    public void deleteResources(String moduleCode, String key) {
        remove(Wrappers.lambdaQuery(I18nResourcePO.class).eq(I18nResourcePO::getModuleCode, moduleCode).eq(I18nResourcePO::getI18nKey, key));
        EventBus.publishEvent(new ResourceSignleRefreshEvent(this, key));
    }

    /**
     * 删除资源
     * @param moduleCode
     */
    @Transactional(rollbackFor = Throwable.class)
    public void deleteResources(String moduleCode) {
        remove(Wrappers.lambdaQuery(I18nResourcePO.class).eq(I18nResourcePO::getModuleCode, moduleCode));
    }

    /**
     * 删除资源
     * @param languageCode
     */
    @Transactional(rollbackFor = Throwable.class)
    public void deleteResourceByLanguageCode(String languageCode) {
        remove(Wrappers.lambdaQuery(I18nResourcePO.class).eq(I18nResourcePO::getLanguageCode, languageCode));
    }
}
