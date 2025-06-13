package com.supos.uns;

import com.supos.common.dto.PageResultDTO;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.service.UnsQueryService;
import com.supos.uns.service.UnsTemplateService;
import com.supos.uns.vo.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 模板管理
 */
@RestController
@Slf4j
@Hidden
public class UnsTemplateApiController {

    @Autowired
    UnsTemplateService unsTemplateService;
    @Autowired
    UnsQueryService unsQueryService;

    @Operation(summary = "查询模板列表", tags = {"openapi.tag.template.management"})
    @PostMapping(path = {"/inter-api/supos/uns/template/pageList", "/open-api/supos/uns/template/pageList"})
    public PageResultDTO<TemplateSearchResult> templatePageList(@Valid @RequestBody TemplateQueryVo params) {
        return unsQueryService.templatePageList(params);
    }

    @Operation(summary = "根据ID查询模板详情", tags = "openapi.tag.template.management")
    @GetMapping(path = {"/inter-api/supos/uns/template", "/open-api/supos/uns/template"})
    public ResultVO<TemplateVo> templateDetail(@RequestParam(name = "id") @Parameter(description = "模板ID") Long id) {
        TemplateVo vo = unsQueryService.getTemplateById(id);
        if (vo == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }
        return ResultVO.successWithData(vo);
    }

    @Operation(summary = "根据别名查询模板详情", tags = {"openapi.tag.template.management"})
    @GetMapping(path = {"/inter-api/supos/uns/template/alias", "/open-api/supos/uns/template/alias"})
    public ResultVO<TemplateVo> templateDetailByAlias(@RequestParam(name = "alias") @Parameter(description = "模板别名") String alias) {
        TemplateVo vo = unsQueryService.getTemplateByAlias(alias);
        if (vo == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }
        return ResultVO.successWithData(vo);
    }

    @Operation(summary = "新增模板", tags = {"openapi.tag.template.management"})
    @PostMapping(path = {"/inter-api/supos/uns/template", "/open-api/supos/uns/template"})
    public ResultVO<String> createTemplate(@Valid @RequestBody CreateTemplateVo createTemplateVo) {
        return unsTemplateService.createTemplate(createTemplateVo);
    }

    @Operation(summary = "修改模板基本信息", tags = "openapi.tag.template.management")
    @PutMapping(path = {"/inter-api/supos/uns/template", "/open-api/supos/uns/template"})
    public ResultVO updateTemplate(@RequestParam(name = "id") @Parameter(description = "模板ID") Long id,
                                   @RequestParam(name = "name", required = false) @Parameter(description = "模板名称") String name,
                                   @RequestParam(name = "description", required = false) @Parameter(description = "模板描述") String description) {
        return unsTemplateService.updateTemplate(id, name, description);
    }

    @Operation(summary = "修改模板字段（只支持删除和新增）和描述", tags = "openapi.tag.folder.management")
    @PutMapping(path = {"/inter-api/supos/uns/model", "/open-api/supos/uns/model"})
    @Valid
    public ResultVO updateFieldAndDesc(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "模板字段定义") UpdateModeRequestVo dto) throws Exception {
        if (dto.getFields() != null && dto.getFields().length > 0) {
            return unsTemplateService.updateFields(dto.getAlias(), dto.getFields());
        } else {
            return unsTemplateService.updateDescription(dto.getAlias(), dto.getModelDescription());
        }
    }

    @Operation(summary = "删除模板", tags = "openapi.tag.template.management")
    @DeleteMapping(path = {"/inter-api/supos/uns/template", "/open-api/supos/uns/template"})
    public ResultVO deleteTemplate(@RequestParam(name = "id") @Parameter(description = "模板ID") Long id) {
        RemoveResult result = unsTemplateService.deleteTemplate(id);
        if (result.getCode() != 0) {
            return ResultVO.fail(result.getMsg());
        }
        return ResultVO.success("ok");
    }

    @Operation(summary = "分页获取模板下的文件列表", tags = "openapi.tag.label.management")
    @GetMapping(path = {"/inter-api/supos/uns/label/pageListUnsByTemplate"})
    public PageResultDTO<FileVo> pageListUnsByTemplate(@RequestParam(name = "templateId") Long templateId,
                                                       @RequestParam(defaultValue = "1", required = false) Long pageNo,
                                                       @RequestParam(defaultValue = "20", required = false) Long pageSize) {
        return unsTemplateService.pageListUnsByTemplate(templateId, pageNo, pageSize);
    }
}
