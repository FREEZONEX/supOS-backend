package com.supos.uns.openapi;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.supos.common.adpater.historyquery.UnsHistoryQueryResult;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.*;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.LabelVo;
import com.supos.common.vo.PersonConfigVo;
import com.supos.common.vo.UserInfoVo;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.openapi.dto.*;
import com.supos.uns.openapi.dto.CreateFileDto;
import com.supos.uns.openapi.dto.CreateFolderDto;
import com.supos.uns.openapi.service.UnsOpenapiService;
import com.supos.uns.openapi.vo.FileDetailVo;
import com.supos.uns.openapi.vo.FolderDetailVo;
import com.supos.uns.openapi.vo.LabelOpenVo;
import com.supos.uns.openapi.vo.OpenTemplateVo;
import com.supos.uns.service.*;
import com.supos.uns.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UNS open-api
 */
@Slf4j
@RestController
public class UnsOpenApi {

    @Autowired
    UnsOpenapiService unsOpenapiService;
    @Autowired
    UnsManagerService unsManagerService;
    @Autowired
    UnsAddService unsAddService;
    @Autowired
    UnsRemoveService unsRemoveService;
    @Autowired
    UnsTemplateService unsTemplateService;
    @Autowired
    UnsQueryService unsQueryService;
    @Autowired
    DbToUnsService dbToUnsService;
    @Autowired
    UnsMapper unsMapper;
    @Autowired
    UnsDataService unsDataService;
    @Autowired
    UnsExternalTopicService unsExternalTopicService;
    @Autowired
    UnsTreeService unsTreeService;
    @Autowired
    UnsLabelService unsLabelService;
    @Autowired
    SystemConfig systemConfig;
    @Autowired
    PersonConfigService personConfigService;

    @Operation(summary = "查询文件夹schema 元数据结构", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/folder/schema","/open-api/uns/folder/schema"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> folderSchema() {
        String lang = getLang();
        return ResponseEntity.ok(ResourceUtil.readUtf8Str("templates/uns/folder-schema" + lang + ".json"));
    }

    @Operation(summary = "查询文件schema 元数据结构", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/file/schema","/open-api/uns/file/schema"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fileSchema() {
        String lang = getLang();
        return ResponseEntity.ok(ResourceUtil.readUtf8Str("templates/uns/file-schema" + lang + ".json"));
    }

    @Operation(summary = "查询模板schema 元数据结构", tags = "openapi.tag.template.management")
    @GetMapping(path = {"/inter-api/supos/uns/template/schema","/open-api/uns/template/schema"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> templateSchema() {
        String lang = getLang();
        return ResponseEntity.ok(ResourceUtil.readUtf8Str("templates/uns/template-schema" + lang + ".json"));
    }

    @Operation(summary = "查询标签schema 元数据结构", tags = "openapi.tag.label.management")
    @GetMapping(path = {"/inter-api/supos/uns/label/schema","/open-api/uns/label/schema"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> labelSchema() {
        String lang = getLang();
        return ResponseEntity.ok(ResourceUtil.readUtf8Str("templates/uns/label-schema" + lang + ".json"));
    }

    @Operation(summary = "别名查询文件夹详情", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/open-api/uns/folder/{alias}"})
    public ResultVO<FolderDetailVo> folderDetailByAlias(@PathVariable @Parameter(description = "别名") String alias) {
        return unsOpenapiService.folderDetailByAlias(alias);
    }

    @Operation(summary = "路径查询文件夹详情", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/open-api/uns/folder/byPath"})
    public ResultVO<FolderDetailVo> folderDetailByPath(@RequestParam @Parameter(description = "路径") String path) {
        return unsOpenapiService.folderDetailByPath(path);
    }

    @Operation(summary = "别名查询文件详情", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/open-api/uns/file/{alias}"})
    public ResultVO<FileDetailVo> fileDetailByAlias(@PathVariable @Parameter(description = "别名") String alias) {
        return unsOpenapiService.fileDetailByAlias(alias);
    }

    @Operation(summary = "路径查询文件详情", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/open-api/uns/file/byPath"})
    public ResultVO<FileDetailVo> fileDetailByPath(@RequestParam @Parameter(description = "路径") String path) {
        return unsOpenapiService.fileDetailByPath(path);
    }

    @Operation(summary = "创建文件夹", tags = "openapi.tag.folder.management")
    @PostMapping(path = {"/open-api/uns/folder"})
    @Valid
    public ResultVO createFolder(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "文件夹字段定义") CreateFolderDto dto) {
        return unsOpenapiService.createFolder(dto);
    }

    @Operation(summary = "创建文件", tags = "openapi.tag.folder.management")
    @PostMapping(path = {"/open-api/uns/file"})
    @Valid
    public ResultVO createFile(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "文件字段定义") CreateFileDto dto) {
        return unsOpenapiService.createFile(dto);
    }


    @Operation(summary = "修改文件夹", tags = "openapi.tag.folder.management")
    @PutMapping(path = {"/open-api/uns/folder/detail/{alias}"})
    @Valid
    public ResultVO updateFolder(@PathVariable @Parameter(description = "别名") String alias,
                                 @RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "文件夹字段定义") UpdateFolderDto dto) {
        return unsOpenapiService.updateFolder(alias, dto);
    }

    @Operation(summary = "修改文件", tags = "openapi.tag.folder.management")
    @PutMapping(path = {"/open-api/uns/file/detail/{alias}"})
    @Valid
    public ResultVO updateFile(@PathVariable @Parameter(description = "别名") String alias,
                               @RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "文件字段定义") UpdateFileDto dto) {
        return unsOpenapiService.updateFile(alias, dto);
    }

    @Operation(summary = "根据别名集合批量删除文件夹和文件" , tags = {"openapi.tag.pride"})
    @DeleteMapping({"/open-api/uns/batch/alias"})
    public ResponseEntity<RemoveResult> batchRemoveResultByAliasList(@RequestBody BatchRemoveUnsDto batchRemoveUnsDto) {
        return unsRemoveService.batchRemoveResultByAliasList(batchRemoveUnsDto);
    }

    @Operation(summary = "批量查询文件实时值", tags = {"openapi.tag.folder.management"})
    @PostMapping(path = {"/open-api/uns/file/current/batchQuery"})
    public ResponseEntity<ResultVO> batchQueryFile(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "别名数组") List<String> aliasList) {
        return unsQueryService.batchQueryFile(aliasList);
    }

    @Operation(summary = "根据文件路径批量查询文件实时值", tags = {"openapi.tag.folder.management"})
    @PostMapping(path = {"/open-api/uns/file/current/batchQuery/byPath"})
    public ResultVO batchQueryFileByPath(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "路径数组") List<String> pathList) {
        return unsOpenapiService.batchQueryFileByPath(pathList);
    }

    @Operation(summary = "批量写文件实时值", tags = {"openapi.tag.folder.management"})
    @PostMapping(path = {"/open-api/uns/file/current/batchUpdate"})
    public ResponseEntity<ResultVO<UnsDataResponseVo>> batchUpdateFile(@RequestBody List<UpdateFileDTO> list) {
        return ResponseEntity.ok(unsDataService.batchUpdateFile(list));
    }

    @Operation(summary = "多条件分页查询树结构", tags = {"openapi.tag.folder.management"})
    @PostMapping(path = {"/open-api/uns/condition/tree"})
    public PageResultDTO<TopicTreeResult> unsTreeByDefinitions(@RequestBody UnsTreeCondition params) {
        return unsTreeService.tree(params);
    }

    /**
     * 基于pride历史查询需求实现  @安冬
     * 91669 【open-api-批量查询文件历史值】需要提供一个标准接口，可以查询文件下“键”名非value的文件历史值，目前定制给PRIDE的接口，只能查询键名为value的历史值
     */
    @Operation(summary = "批量查询文件历史值", tags = {"openapi.tag.folder.management"})
    @PostMapping(path = {"/open-api/uns/file/history/batch/query"})
    public ResponseEntity<ResultVO<UnsHistoryQueryResult>> batchQueryFileHistoryValue(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "历史值查询请求参数") HistoryValueRequest historyValueRequest) {
        return unsDataService.batchQueryFileHistoryValue(historyValueRequest);
    }

    @Operation(summary = "查询模板列表", tags = {"openapi.tag.template.management"})
    @GetMapping(path = {"/open-api/uns/template"})
    public PageResultDTO<TemplateSearchResult> templatePageList(@ParameterObject TemplateQueryVo params) {
        return unsQueryService.templatePageList(params);
    }

    @Operation(summary = "查询模板详情", tags = {"openapi.tag.template.management"})
    @GetMapping(path = {"/open-api/uns/template/{alias}"})
    public ResultVO<OpenTemplateVo> templateDetailByAlias(@PathVariable(name = "alias") @Parameter(description = "模板别名") String alias) {
        TemplateVo vo = unsQueryService.getTemplateByAlias(alias);
        if (vo == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }
        OpenTemplateVo openTemplateVo = BeanUtil.copyProperties(vo, OpenTemplateVo.class);
        openTemplateVo.setDefinition(vo.getFields());
        return ResultVO.successWithData(openTemplateVo);
    }

    /**
     * 此接口pride在用
     */
    @Operation(summary = "新增模板", tags = {"openapi.tag.template.management"})
    @PostMapping(path = {"/open-api/uns/template"})
    public ResultVO<String> createTemplate(@Valid @RequestBody CreateTemplateVo createTemplateVo) {
        return unsTemplateService.createTemplate(createTemplateVo);
    }

    @Operation(summary = "修改模板", tags = "openapi.tag.template.management")
    @PutMapping(path = {"/open-api/uns/template/{alias}"})
    @Valid
    public ResultVO updateTemplate(@PathVariable @Parameter(description = "别名") String alias,
                                   @RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "模板更新对象") UpdateTemplateDto dto) {
        return unsOpenapiService.updateTemplate(alias, dto);
    }

    @Operation(summary = "删除模板", tags = "openapi.tag.template.management")
    @DeleteMapping(path = {"/open-api/uns/template/{alias}"})
    public ResultVO deleteTemplate(@PathVariable(name = "alias") @Parameter(description = "别名") String alias) {
        UnsPo template = unsMapper.getByAlias(alias);
        if (null == template) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }

        RemoveResult result = unsTemplateService.deleteTemplate(template.getId());
        if (result.getCode() != 0) {
            return ResultVO.fail(result.getMsg());
        }
        return ResultVO.success("ok");
    }

    @Operation(summary = "标签列表", description = "列出所有支持的标签，支持模糊搜索", tags = "openapi.tag.label.management")
    @GetMapping(path = {"/open-api/uns/label"})
    public PageResultDTO<LabelOpenVo> allLabels(LabelQueryDto queryDto) {
        return unsOpenapiService.allLabels(queryDto);
    }

    @Operation(summary = "标签详情", tags = "openapi.tag.label.management")
    @GetMapping(path = {"/open-api/uns/label/{id}"})
    public ResultVO<LabelVo> labelDetail(@PathVariable(name = "id") @Parameter(description = "标签ID") String id) {
        try {
            return unsLabelService.detail(Long.parseLong(id));
        } catch (NumberFormatException e) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.not.exists"));
        }
    }

    @Operation(summary = "创建标签", tags = "openapi.tag.label.management")
    @PostMapping(path = {"/open-api/uns/label"})
    public ResultVO<LabelVo> createLabel(@Valid @RequestBody CreateLabelDto createLabelDto) {
        return unsLabelService.create(createLabelDto.getLabelName());
    }

    @Operation(summary = "修改标签", tags = "openapi.tag.label.management")
    @PutMapping(path = {"/open-api/uns/label/{id}"})
    public ResultVO updateLabel(@PathVariable(name = "id") @Parameter(description = "标签ID") Long id,
                                @Valid @RequestBody UpdateLabelDto dto) {
        try {
            dto.setId(String.valueOf(id));
        } catch (Exception e) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.not.exists"));
        }
        return unsLabelService.update(dto);
    }

    @Operation(summary = "删除标签", tags = "openapi.tag.label.management")
    @DeleteMapping(path = {"/open-api/uns/label/{id}"})
    public ResultVO deleteLabel(@PathVariable @Parameter(description = "标签ID") Long id) {
        return unsLabelService.delete(id);
    }

    @Operation(summary = "批量文件打标签", tags = "openapi.tag.label.management")
    @PostMapping(path = {"/open-api/uns/batch/makeLabel"})
    public ResultVO makeLabel(@RequestBody @Parameter(description = "标签集合，为空则取消所有标签") List<MakeLabelDto> makeLabelList) {
        return unsLabelService.batchMakeLabel(makeLabelList);
    }

    @Operation(summary = "文件取消标签", tags = "openapi.tag.label.management")
    @PutMapping(path = {"/open-api/uns/cancelLabel/{alias}"})
    public ResultVO cancelLabel(@PathVariable @Parameter(description = "文件别名") String alias,
                                @RequestBody @Parameter(description = "需要取消的标签名称集合") List<String> labelNames) {
        return unsLabelService.cancelLabel(alias, labelNames);
    }





    private String getLang(){
        String lang = "en-US".equals(systemConfig.getLang()) ? "_en" : "";
        UserInfoVo userInfoVo = UserContext.get();
        if (userInfoVo != null) {
            PersonConfigVo personConfigVo = personConfigService.getByUserId(userInfoVo.getSub());
            if (personConfigVo != null) {
                lang = "en-US".equals(personConfigVo.getMainLanguage()) ? "_en" : "";
            }
        }
        return lang;
    }
}