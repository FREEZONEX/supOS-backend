package com.supos.i18n.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.event.EventBus;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.i18n.common.LanguageType;
import com.supos.i18n.dao.mapper.I18nLanguageMapper;
import com.supos.i18n.dao.po.I18nLanguagePO;
import com.supos.i18n.dto.AddLanguageDto;
import com.supos.i18n.dto.LanguageEnableParam;
import com.supos.i18n.event.LanguageDeleteEvent;
import com.supos.i18n.vo.I18nLanguageVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: I18nLanguageService
 * @date 2025/9/1 10:09
 */
@Service
public class I18nLanguageService extends ServiceImpl<I18nLanguageMapper, I18nLanguagePO> {
    private static final int LANGUAGE_CODE_LENGTH_NUM_DEFA = 10;
    private static final int LANGUAGE_NAME_LENGTH_NUM_DEFA = 50;
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("^[a-z]{2}_[A-Z]{2}$");

    @Autowired
    private I18nLanguageMapper i18nLanguageMapper;

    public I18nLanguagePO getByCode(String languageCode) {
        return getOne(Wrappers.lambdaQuery(I18nLanguagePO.class).eq(I18nLanguagePO::getLanguageCode, languageCode), false);
    }

    public List<I18nLanguageVO> getAllLanguage(boolean onlyForEnabled) {
        LambdaQueryWrapper<I18nLanguagePO> queryWrapper = Wrappers.lambdaQuery(I18nLanguagePO.class);
        if (onlyForEnabled) {
            queryWrapper.eq(I18nLanguagePO::getHasUsed, true);
        }
        List<I18nLanguagePO> allLanguage = i18nLanguageMapper.selectList(queryWrapper);
        List<I18nLanguageVO> i18nLanguageVOS = new ArrayList<>();
        for (I18nLanguagePO i18nLanguage : allLanguage) {
            I18nLanguageVO i18nLanguageVO = new I18nLanguageVO();
            i18nLanguageVO.setId(i18nLanguage.getId());
            i18nLanguageVO.setLanguageCode(i18nLanguage.getLanguageCode());
            i18nLanguageVO.setLanguageName(i18nLanguage.getLanguageName());
            i18nLanguageVO.setLanguageType(i18nLanguage.getLanguageType());
            i18nLanguageVO.setHasUsed(i18nLanguage.getHasUsed());
            i18nLanguageVOS.add(i18nLanguageVO);
        }
        return i18nLanguageVOS;
    }

    public Set<String> getAllLanguageCode(boolean onlyForEnabled) {
        List<I18nLanguageVO> list = getAllLanguage(onlyForEnabled);
        return list.stream().map(I18nLanguageVO::getLanguageCode).collect(Collectors.toSet());
    }

    private String checkParam(AddLanguageDto addLanguageDto, boolean throwEx) {
        if (StringUtils.isBlank(addLanguageDto.getLanguageCode())) {
            if (throwEx) {
                throw new BuzException("param.null", "languageCode");
            }else {
                return I18nUtils.getMessage("param.null", "languageCode");
            }
        }
        if (addLanguageDto.getLanguageCode().length() > LANGUAGE_CODE_LENGTH_NUM_DEFA) {
            if (throwEx) {
                throw new BuzException("i18n.exception.length_limit_error", "languageCode", LANGUAGE_CODE_LENGTH_NUM_DEFA);
            }else {
                return I18nUtils.getMessage("i18n.exception.length_limit_error", "languageCode", LANGUAGE_CODE_LENGTH_NUM_DEFA);
            }
        }
        if (!LANGUAGE_PATTERN.matcher(addLanguageDto.getLanguageCode()).matches()) {
            if (throwEx) {
                throw new BuzException("i18n.language_format_is_incorrect");
            }else {
                return I18nUtils.getMessage("i18n.language_format_is_incorrect");
            }
        }
        if (StringUtils.isBlank(addLanguageDto.getLanguageName())) {
            if (throwEx) {
                throw new BuzException("param.null", "languageName");
            }else {
                return I18nUtils.getMessage("param.null", "languageName");
            }
        }
        if (addLanguageDto.getLanguageName().length() > LANGUAGE_NAME_LENGTH_NUM_DEFA) {
            if (throwEx) {
                throw new BuzException("i18n.exception.length_limit_error", "languageName", LANGUAGE_NAME_LENGTH_NUM_DEFA);
            }else {
                return I18nUtils.getMessage("i18n.exception.length_limit_error", "languageName", LANGUAGE_NAME_LENGTH_NUM_DEFA);
            }
        }
        return null;
    }

    @Transactional(rollbackFor = Throwable.class)
    public Map<String, String> saveLanguage(AddLanguageDto addLanguageDto, boolean throwEx) {
        Map<String, String> errorMap = new HashMap<>();

        String error = null;
        error = checkParam(addLanguageDto, throwEx);
        if (error != null) {
            errorMap.put(addLanguageDto.getFlagNo(), error);
        }
        if (!errorMap.isEmpty()) {
            return errorMap;
        }

        I18nLanguagePO existLanguage = getByCode(addLanguageDto.getLanguageCode());
        if (existLanguage == null) {
            I18nLanguagePO i18nLanguagePO = new I18nLanguagePO();
            i18nLanguagePO.setLanguageCode(addLanguageDto.getLanguageCode());
            i18nLanguagePO.setLanguageName(addLanguageDto.getLanguageName());
            if (addLanguageDto.getLanguageType() == null) {
                i18nLanguagePO.setLanguageType(LanguageType.CUSTOM.getType());
            } else {
                i18nLanguagePO.setLanguageType(addLanguageDto.getLanguageType());
            }
            if (addLanguageDto.getHasUsed() == null) {
                i18nLanguagePO.setHasUsed(false);
            } else {
                i18nLanguagePO.setHasUsed(addLanguageDto.getHasUsed());
            }

            save(i18nLanguagePO);
        } else {
            existLanguage.setLanguageName(addLanguageDto.getLanguageName());
            updateById(existLanguage);
        }
        return errorMap;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void languageEnable(LanguageEnableParam languageEnableParam) {
        // 必须启用至少一个语言
        if (!languageEnableParam.getEnable()) {
            List<I18nLanguagePO> allUsedLanguage = list(Wrappers.lambdaQuery(I18nLanguagePO.class).eq(I18nLanguagePO::getHasUsed, true));
            if (allUsedLanguage.size() == 1 && languageEnableParam.getLanguageCode().equals(allUsedLanguage.get(0).getLanguageCode())) {
                throw new BuzException("i18n.exception.language_has_used_error");
            }
        }

        I18nLanguagePO i18nLanguagePO = getOne(Wrappers.lambdaQuery(I18nLanguagePO.class).eq(I18nLanguagePO::getLanguageCode, languageEnableParam.getLanguageCode()), false);
        if (i18nLanguagePO != null) {
            i18nLanguagePO.setHasUsed(languageEnableParam.getEnable());
        }

        updateById(i18nLanguagePO);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void deleteLanguage(String languageCode) {
        remove(Wrappers.lambdaQuery(I18nLanguagePO.class).eq(I18nLanguagePO::getLanguageCode, languageCode));
        EventBus.publishEvent(new LanguageDeleteEvent(this, languageCode));
    }
}
