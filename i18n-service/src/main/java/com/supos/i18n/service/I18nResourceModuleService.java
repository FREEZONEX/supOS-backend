package com.supos.i18n.service;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.i18n.common.Constants;
import com.supos.i18n.common.ModuleType;
import com.supos.i18n.dao.mapper.I18nResourceModuleMapper;
import com.supos.i18n.dao.po.I18nResourceModulePO;
import com.supos.i18n.dao.po.I18nResourcePO;
import com.supos.i18n.dto.AddModuleDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: I18nResourceModuleService
 * @date 2025/9/2 10:19
 */
@Service
public class I18nResourceModuleService extends ServiceImpl<I18nResourceModuleMapper, I18nResourceModulePO> {

    private static final int MODULE_CODE_LENGTH_NUM_DEFA = 128;
    private static final int MODULE_NAME_LENGTH_NUM_DEFA = 200;
    private static final Pattern MODULE_CODE_PATTERN = Pattern.compile("^[a-zA-Z\\d_-]+$");

    @Autowired
    private I18nResourceModuleMapper i18nResourceModuleMapper;

    @Autowired
    private I18nResourceService i18nResourceService;

    /**
     * 获取模块
     * @param moduleCode
     * @return
     */
    public I18nResourceModulePO getModule(String moduleCode) {
        return getOne(Wrappers.lambdaQuery(I18nResourceModulePO.class).eq(I18nResourceModulePO::getModuleCode, moduleCode), false);
    }

    /**
     * 获取所有模块编号
     * @return
     */
    public Set<String> getAllModuleCode() {
        List<I18nResourceModulePO> allModule = getAllModule();
        if (CollectionUtil.isNotEmpty(allModule)) {
            return allModule.stream().map(I18nResourceModulePO::getModuleCode).collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    /**
     * 获取所有模块
     * @return
     */
    public List<I18nResourceModulePO> getAllModule() {
        List<I18nResourceModulePO> allModule = i18nResourceModuleMapper.selectList(Wrappers.lambdaQuery(I18nResourceModulePO.class));
        return allModule;
    }

    /**
     * 批量获取模块
     * @return
     */
    public List<I18nResourceModulePO> getModules(Collection<String> moduleCodes) {
        if (CollectionUtil.isEmpty(moduleCodes)) {
            return new ArrayList<>();
        }
        List<I18nResourceModulePO> allModule = i18nResourceModuleMapper.selectList(Wrappers.lambdaQuery(I18nResourceModulePO.class)
                .in(I18nResourceModulePO::getModuleCode, moduleCodes));
        return allModule;
    }

    /**
     * 查询模块
     * @param keyword
     * @return
     */
    public List<I18nResourceModulePO> queryModules(String keyword, Integer moduleType) {

        List<I18nResourceModulePO> modules = new ArrayList<>();

        // 先搜key
        List<String> keys = new ArrayList<>();
        if (keyword != null && keyword != "") {
            String language = LocaleContextHolder.getLocale().getLanguage();
            String languageCode = Constants.LANGUAGE_MAP.get(language);
            if (languageCode != null) {
                language = languageCode;
            }
            List<I18nResourcePO> resources = i18nResourceService.getResourceByLanguageAndValue(language, keyword);
            if (CollectionUtil.isNotEmpty(resources)) {
                keys.addAll(resources.stream().map(I18nResourcePO::getI18nKey).collect(Collectors.toSet()));
            } else {
                return modules;
            }
        }

        if (keyword != null && keyword != "") {
            List<List<String>> subKeys = Lists.partition(keys, 500);
            for (List<String> subKey : subKeys) {
                LambdaQueryWrapper<I18nResourceModulePO> qw = new LambdaQueryWrapper<>();
                if (StringUtils.isNotBlank(keyword)) {
                    qw.in(I18nResourceModulePO::getModuleName, subKey);
                }
                if (moduleType != null) {
                    qw.eq(I18nResourceModulePO::getModuleType, moduleType);
                }
                List<I18nResourceModulePO> modulePOs = i18nResourceModuleMapper.selectList(qw);
                if (CollectionUtil.isNotEmpty(modulePOs)) {
                    modules.addAll(modulePOs);
                }
            }
        } else {
            LambdaQueryWrapper<I18nResourceModulePO> qw = new LambdaQueryWrapper<>();
            if (moduleType != null) {
                qw.eq(I18nResourceModulePO::getModuleType, moduleType);
            }
            List<I18nResourceModulePO> modulePOs = i18nResourceModuleMapper.selectList(qw);
            if (CollectionUtil.isNotEmpty(modulePOs)) {
                modules.addAll(modulePOs);
            }
        }

        for (I18nResourceModulePO modulePO : modules) {
            modulePO.setModuleName(I18nUtils.getMessage(modulePO.getModuleName()));
        }
        return modules;
    }

    /**
     * 新增模块
     * @param addModuleDto
     * @return
     */
    @Transactional(rollbackFor = Throwable.class)
    public Long addModule(AddModuleDto addModuleDto) {
        checkModuleCode(addModuleDto.getModuleCode(), true);
        checkModuleName(addModuleDto.getModuleName(), true);

        I18nResourceModulePO i18nResourceModulePO = new I18nResourceModulePO();
        i18nResourceModulePO.setModuleCode(addModuleDto.getModuleCode());
        i18nResourceModulePO.setModuleName(addModuleDto.getModuleName());
        i18nResourceModulePO.setModuleType(addModuleDto.getModuleType());
        save(i18nResourceModulePO);
        return i18nResourceModulePO.getId();
    }

    /**
     * 批量新增模块
     * @param addModuleDtos
     * @return
     */
    @Transactional(rollbackFor = Throwable.class)
    public Map<String, String> addModule(Collection<AddModuleDto> addModuleDtos, boolean throwEx) {
        Map<String, String> errorMap = new HashMap<>();

        List<I18nResourceModulePO> existModules = getAllModule();
        Map<String, I18nResourceModulePO> existModuleMap = existModules.stream().collect(Collectors.toMap(I18nResourceModulePO::getModuleCode, Function.identity(), (k1, k2) -> k2));

        List<I18nResourceModulePO> addModules = new ArrayList<>();
        List<I18nResourceModulePO> updateModules = new ArrayList<>();
        for (AddModuleDto addModuleDto : addModuleDtos) {
            String error = null;
            // 校验模块编码
            error = checkModuleCode(addModuleDto.getModuleCode(), throwEx);
            if (error != null) {
                errorMap.put(addModuleDto.getFlagNo(), error);
                continue;
            }

            // 校验模块名称
            error = checkModuleName(addModuleDto.getModuleName(), throwEx);
            if (error != null) {
                errorMap.put(addModuleDto.getFlagNo(), error);
                continue;
            }

            I18nResourceModulePO existModule = existModuleMap.get(addModuleDto.getModuleCode());
            if (existModule == null) {
                I18nResourceModulePO i18nResourceModulePO = new I18nResourceModulePO();
                i18nResourceModulePO.setModuleCode(addModuleDto.getModuleCode());
                i18nResourceModulePO.setModuleName(addModuleDto.getModuleName());
                i18nResourceModulePO.setModuleType(addModuleDto.getModuleType());
                addModules.add(i18nResourceModulePO);
            } else {
                if (ModuleType.getByType(existModule.getModuleType()) != ModuleType.BUILTIN) {
                    // 非内置模块，不允许修改
                    if (!StringUtils.equals(existModule.getModuleName(), addModuleDto.getModuleName())) {
                        existModule.setModuleName(addModuleDto.getModuleName());
                        updateModules.add(existModule);
                    }
                }
            }

        }

        if (CollectionUtil.isNotEmpty(addModules)) {
            saveBatch(addModules);
        }
        if (CollectionUtil.isNotEmpty(updateModules)) {
            updateBatchById(updateModules);
        }
        return errorMap;
    }

    private String checkModuleCode(String moduleCode, boolean throwEx) {
        if (StringUtils.isBlank(moduleCode)) {
            if (throwEx) {
                throw new BuzException("param.null", "moduleCode");
            }else {
                return I18nUtils.getMessage("param.null", "moduleCode");
            }
        }
        if (moduleCode.length() > MODULE_CODE_LENGTH_NUM_DEFA) {
            if (throwEx) {
                throw new BuzException("i18n.exception.length_limit_error", "moduleCode", MODULE_CODE_LENGTH_NUM_DEFA);
            } else {
                return I18nUtils.getMessage("i18n.exception.length_limit_error", "moduleCode", MODULE_CODE_LENGTH_NUM_DEFA);
            }
        }

        if (!MODULE_CODE_PATTERN.matcher(moduleCode).matches()) {
            if (throwEx) {
                throw new BuzException("i18n.exception.module_code_format_error");
            } else {
                return I18nUtils.getMessage("i18n.exception.module_code_format_error");
            }
        }
        return null;
    }

    private String checkModuleName(String moduleName, boolean throwEx) {
        if (StringUtils.isBlank(moduleName)) {
            if (throwEx) {
                throw new BuzException("param.null", "moduleName");
            }else {
                return I18nUtils.getMessage("param.null", "moduleName");
            }
        }
        if (moduleName.length() > MODULE_NAME_LENGTH_NUM_DEFA) {
            if (throwEx) {
                throw new BuzException("i18n.exception.length_limit_error", "moduleName", MODULE_NAME_LENGTH_NUM_DEFA);
            } else {
                return I18nUtils.getMessage("i18n.exception.length_limit_error", "moduleName", MODULE_NAME_LENGTH_NUM_DEFA);
            }
        }

        return null;
    }

    /**
     * 删除模块
     * @param moduleCode
     * @return
     */
    @Transactional(rollbackFor = Throwable.class)
    public void deleteModule(String moduleCode) {
        remove(Wrappers.lambdaQuery(I18nResourceModulePO.class).eq(I18nResourceModulePO::getModuleCode, moduleCode));
    }
}
