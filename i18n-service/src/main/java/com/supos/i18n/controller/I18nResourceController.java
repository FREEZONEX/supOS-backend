package com.supos.i18n.controller;

import com.supos.common.dto.PageResultDTO;
import com.supos.common.exception.vo.ResultVO;
import com.supos.i18n.dto.SaveResourceDto;
import com.supos.i18n.dto.I18nResourceQuery;
import com.supos.i18n.service.I18nResourceService;
import com.supos.i18n.vo.I18nResourceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化资源相关接口
 * @date 2025/9/1 14:05
 */
@RestController
@RequestMapping("/inter-api/supos/i18n/resources")
public class I18nResourceController {

    @Autowired
    private I18nResourceService i18nResourceService;

    /**
     * 搜索国际化资源
     * @param query
     * @return
     */
    @PostMapping("/search")
    public PageResultDTO<I18nResourceVO> search(@Validated @RequestBody I18nResourceQuery query) {
        return i18nResourceService.search( query);
    }

    /**
     * 新增国际化资源
     * @param resource
     * @return
     */
    @PostMapping
    public ResultVO addResources(@RequestBody SaveResourceDto resource) {
        i18nResourceService.addResources(resource);
        return ResultVO.success();
    }

    /**
     * 修改国际化资源
     * @param resource
     * @return
     */
    @PutMapping()
    public ResultVO updateResource(@RequestBody SaveResourceDto resource) {
        i18nResourceService.updateResources(resource);
        return ResultVO.success();
    }

    /**
     * 删除国际化资源
     * @param moduleCode
     * @param key
     * @return
     */
    @DeleteMapping("/{moduleCode}/{key}")
    public ResultVO deleteResources(@PathVariable String moduleCode, @PathVariable("key") String key) {
        i18nResourceService.deleteResources(moduleCode, key);
        return ResultVO.success();
    }
}
