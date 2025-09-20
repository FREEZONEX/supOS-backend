package com.supos.i18n.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.supos.common.exception.vo.ResultVO;
import com.supos.i18n.dao.po.I18nResourceModulePO;
import com.supos.i18n.service.I18nManagerService;
import com.supos.i18n.service.I18nResourceModuleService;
import com.supos.i18n.vo.I18nResourceModuleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化模块相关接口
 * @date 2025/9/4 16:34
 */
@RestController
@RequestMapping("/inter-api/supos/i18n/modules")
public class I18nModuleController {

    @Autowired
    private I18nResourceModuleService i18nResourceModuleService;

    @Autowired
    private I18nManagerService i18nManagerService;

    /**
     * 查询模块
     * @return
     */
    @GetMapping
    public ResultVO<List<I18nResourceModuleVO>> queryModule(@RequestParam(name = "keyword",required = false) String keyword, @RequestParam(name = "moduleType",required = false) Integer moduleType) {
        List<I18nResourceModulePO> modules = i18nResourceModuleService.queryModules(keyword, moduleType);
        if (CollectionUtil.isNotEmpty(modules)) {
            return ResultVO.successWithData(modules.stream().map(module -> {
                I18nResourceModuleVO moduleVO = new I18nResourceModuleVO();
                moduleVO.setId(module.getId());
                moduleVO.setModuleCode(module.getModuleCode());
                moduleVO.setModuleName(module.getModuleName());
                moduleVO.setModuleType(module.getModuleType());
                return moduleVO;
            }).toList());
        }
        return ResultVO.successWithData(new ArrayList<>());
    }

    /**
     * 删除语言
     */
    @DeleteMapping("/{code}")
    public ResultVO deleteModule(@PathVariable("code") String code) {
        i18nManagerService.deleteModule(code);
        return ResultVO.success();
    }
}
