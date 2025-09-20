package com.supos.i18n.controller;

import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.i18n.dto.AddLanguageDto;
import com.supos.i18n.dto.LanguageEnableParam;
import com.supos.i18n.service.I18nLanguageService;
import com.supos.i18n.service.I18nManagerService;
import com.supos.i18n.vo.I18nLanguageVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化语言相关接口
 * @date 2025/9/1 10:31
 */
@RestController
@RequestMapping("/inter-api/supos/i18n/languages")
public class I18nLanguageController {

    @Autowired
    private I18nLanguageService i18nLanguageService;

    @Autowired
    private I18nManagerService i18nManagerService;

    /**
     * 获取所有语言
     * @return
     */
    @GetMapping
    public ResultVO<List<I18nLanguageVO>> languageObtain(@RequestParam(name = "onlyForEnabled",required = false,defaultValue = "false") Boolean onlyForEnabled) {
        return ResultVO.successWithData(i18nLanguageService.getAllLanguage(onlyForEnabled));
    }

    /**
     * 新增语言
     */
    @PostMapping
    public ResultVO addLanguage(@Valid @RequestBody AddLanguageDto addLanguageDto) {
        i18nLanguageService.saveLanguage(addLanguageDto,  true);
        return ResultVO.success();
    }

    /**
     * 启用/停用语言
     */
    @PutMapping("/enable")
    public ResultVO languageEnable(@RequestBody LanguageEnableParam languageEnableParam) {
        i18nLanguageService.languageEnable(languageEnableParam);
        return ResultVO.success();
    }

    /**
     * 删除语言
     */
    @DeleteMapping("/{code}")
    public ResultVO deleteLanguage(@PathVariable("code") String code) {
        i18nManagerService.deleteLanguage(code);
        return ResultVO.success();
    }
}
