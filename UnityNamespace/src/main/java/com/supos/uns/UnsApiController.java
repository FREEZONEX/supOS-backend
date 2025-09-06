package com.supos.uns;

import com.supos.common.Constants;
import com.supos.common.NodeType;
import com.supos.common.adpater.historyquery.UnsHistoryQueryResult;
import com.supos.common.dto.*;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.service.*;
import com.supos.uns.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
public class UnsApiController {

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

    private static final Set<Integer> baseDataTypes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(Constants.TIME_SEQUENCE_TYPE, Constants.RELATION_TYPE)));

    @Operation(summary = "分页搜索主题")
    @GetMapping(path = {"/inter-api/supos/uns/search"}, produces = "application/json")
    public TopicPaginationSearchResult searchPaged(@RequestParam(name = "k", required = false) @Parameter(description = "模糊搜索词") String key,
                                                   @RequestParam(name = "modelTopic", required = false) @Parameter(description = "模型topic") String modelTopic,
                                                   @RequestParam(name = "type", required = false, defaultValue = "2") @Parameter(description = "搜索类型: 1--模型, 2--文件(默认值), 3--非计算时序文件，4--时序文件，5--报警规则") int searchType,
                                                   @RequestParam(name = "normal", required = false) @Parameter(description = "只搜索基本文件") boolean normal,
                                                   @RequestParam(name = "dataTypes", required = false) @Parameter(description = "数据类型") Set<Integer> dataTypes,
                                                   @RequestParam(name = "pageNo", required = false, defaultValue = "1") @Parameter(description = "页码，默认1") Integer pageNo,
                                                   @RequestParam(name = "pageSize", required = false, defaultValue = "10") @Parameter(description = "每页条数，默认10") Integer pageSize,
                                                   @RequestParam(name = "nfc", required = false) @Parameter(description = "至少需要的数字类型字段的个数") Integer nfc) throws Exception {
        NodeType nodeType = NodeType.valueOf(searchType);
        if (normal && (dataTypes == null || dataTypes.isEmpty())) {
            dataTypes = baseDataTypes;
        }
        return unsQueryService.searchPaged(modelTopic, key, nodeType, dataTypes, pageNo, pageSize, nfc);
    }

    @Operation(summary = "搜索主题树，默认整个树", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/tree"})
    public JsonResult<List<TopicTreeResult>> searchTree(@RequestParam(name = "key", required = false) @Parameter(description = "子节点模糊搜索词") String keyword,
                                                        @RequestParam(name = "parentId", required = false) @Parameter(description = "父级ID") Long parentId,
                                                        @RequestParam(name = "showRec", required = false, defaultValue = "false") @Parameter(description = "显示记录条数") boolean showRec,
                                                        @RequestParam(name = "type", required = false, defaultValue = "1") @Parameter(description = "搜索类型: 1--文本搜索, 2--标签搜索，3--模板搜索") int searchType,
                                                        @RequestParam(name = "pathType", required = false) @Parameter(description = "路径类型: 0--文件夹，1--模板，2--文件") Integer pathType
    ) throws Exception {
        if (1 == searchType) {
            return unsQueryService.searchTree(keyword, parentId, showRec,pathType);
        } else if (2 == searchType) {
            return unsQueryService.searchByTag(keyword);
        } else if (3 == searchType) {
            return unsQueryService.searchByTemplate(keyword);
        } else {
            return new JsonResult<>(0, unsMapper.timeZone(), Collections.emptyList());
        }
    }

    @Operation(summary = "搜索外部topic主题树，默认整个树" , tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/external/tree"})
    public JsonResult<List<TopicTreeResult>> searchExternalTree(@RequestParam(name = "key", required = false) @Parameter(description = "子节点模糊搜索词") String keyword) {
        List<TopicTreeResult> treeResults = unsQueryService.searchExternalTopics(keyword);
        return new JsonResult<>(0, "ok", treeResults);
    }

    @Operation(summary = "清除所有外部topic" , tags = "openapi.tag.folder.management")
    @DeleteMapping(path = {"/inter-api/supos/external/topic/clear"})
    public JsonResult<String> clearExternalTree() {
        unsQueryService.clearExternalTopicCache();
        return new JsonResult<>(0, "ok");
    }

    @Operation(summary = "多条件分页查询树结构", tags = {"openapi.tag.folder.management"})
    @PostMapping(path = {"/inter-api/supos/uns/condition/tree"})
    public PageResultDTO<TopicTreeResult> unsTreeByDefinitions(@RequestBody UnsTreeCondition params) {
        return unsTreeService.tree(params);
    }

    @Operation(summary = "枚举数据类型", description = "列出所有支持的数据类型，供建表时下拉选择", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/types"})
    public JsonResult<Collection<String>> listTypes() {
        return unsQueryService.listTypes();
    }

    @Operation(summary = "获取最新消息", parameters = @Parameter(name = "topic", description = "主题"), responses = @ApiResponse(description = "消息体"))
    @GetMapping(value = "/inter-api/supos/uns/getLastMsg", produces = "application/json")
    public JsonResult<Object> getLastMsg(
            @RequestParam(name = "path", required = false)
            @Parameter(description = "主题，文件path路径") String[] paths,
            @RequestParam(name = "id", required = false)
            @Parameter(description = "主题id") Long id,
            @RequestParam(name = "alias", required = false)
            @Parameter(description = "主题别名") String alias
    ) throws Exception {
        // 查表最新数据
        if (id != null) {
            String rs = unsQueryService.getLastMsg(id, true).getData();
            if (rs != null) {
                Object jsonObj = JsonUtil.fromJson(rs);
                return new JsonResult<>(0, "ok", jsonObj);
            } else {
                return new JsonResult<>(0, "NoData");
            }
        } else if (alias != null) {
            String rs = unsQueryService.getLastMsgByAlias(alias, true).getData();
            if (rs != null) {
                Object jsonObj = JsonUtil.fromJson(rs);
                return new JsonResult<>(0, "ok", jsonObj);
            } else {
                return new JsonResult<>(0, "NoData");
            }
        }
        if (ArrayUtils.isEmpty(paths)) {
            return new JsonResult<>(0, "Empty topics");
        }
        if (paths.length == 1) {
            String rs = unsQueryService.getLastMsgByPath(paths[0], true).getData();
            if (rs != null) {
                Object jsonObj = JsonUtil.fromJson(rs);
                return new JsonResult<>(0, "ok", jsonObj);
            } else {
                return new JsonResult<>(0, "NoData");
            }
        } else {
            LinkedHashMap<String, Object> msgs = new LinkedHashMap<>();
            for (String topic : paths) {
                String rs = unsQueryService.getLastMsgByPath(topic, true).getData();
                if (rs != null) {
                    Object jsonObj = JsonUtil.fromJson(rs);
                    msgs.put(topic, jsonObj);
                }
            }
            return new JsonResult<>(0, "ok", msgs);
        }
    }

    @Operation(summary = "查询文件夹详情", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/model"})
    public JsonResult<ModelDetail> getModelDefinition(@RequestParam(name = "id") @Parameter(description = "模型对应的主题ID") Long id) throws Exception {
        return unsQueryService.getModelDefinition(id, null);
    }

    @Operation(summary = "查询文件详情", tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/instance"})
    public JsonResult<InstanceDetail> getInstanceDetail(@RequestParam(name = "id") @Parameter(description = "模型对应的主题ID") Long id) throws Exception {
        return unsQueryService.getInstanceDetail(id, null);
    }

    @Operation(summary = "外部数据源表的字段定义转uns字段定义")
    @PostMapping(path = {"/inter-api/supos/uns/ds2fs"})
    public JsonResult<FieldDefine[]> dataSrc2UnsFields(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "数据源字段定义") DbFieldsInfoVo infoVo) throws Exception {
        return dbToUnsService.parseDatabaseFields(infoVo);
    }

    @Operation(summary = "外部JSON定义转uns字段定义")
    @PostMapping(path = {"/inter-api/supos/uns/json2fs"})
    public JsonResult<List<OuterStructureVo>> parseJson2uns(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "json body") String json) {
        return unsQueryService.parseJson2uns(json);
    }

    @Operation(summary = "外部JSON定义转树结构uns字段定义")
    @PostMapping(path = {"/inter-api/supos/uns/json2fs/tree"})
    public JsonResult<List<TreeOuterStructureVo>> parseJson2TreeUns(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "json body") String json) {
        return unsQueryService.parseJson2TreeUns(json);
    }

    @Operation(summary = "创建文件夹和文件", tags = "openapi.tag.folder.management")
    @PostMapping(path = {"/inter-api/supos/uns/model"})
    @Valid
    public JsonResult<String> createModelInstance(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "文件夹或文件字段定义") CreateTopicDto dto) throws Exception {
        return unsAddService.createModelInstance(dto);
    }

    @Operation(summary = "修改文件夹或文件明细", tags = "openapi.tag.folder.management")
    @PutMapping(path = {"/inter-api/supos/uns/detail"})
    @Valid
    public JsonResult<String> updateDetail(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "文件夹或文件字段定义") UpdateUnsDto dto) {
        return unsAddService.updateModelInstance(dto);
    }

    @Operation(summary = "修改文件夹或文件名称", tags = "openapi.tag.folder.management")
    @PutMapping(path = {"/inter-api/supos/uns/name"})
    @Valid
    public JsonResult<String> updateName(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "字段定义") UpdateNameVo updateNameVo) {
        return unsAddService.updateName(updateNameVo);
    }

    @Operation(summary = "预先判断是否有属性关联")
    @PostMapping(path = {"/inter-api/supos/uns/model/detect"})
    @Valid
    public ResultVO detectIfFieldReferenced(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "模型字段定义") UpdateModeRequestVo dto) throws Exception {
        return unsManagerService.detectIfFieldReferenced(dto.getAlias(), dto.getFields());
    }

    @Operation(summary = "批量创建文件夹和文件", tags = "openapi.tag.folder.management")
    @PostMapping(path = {"/inter-api/supos/uns/batch"})
    public ResultVO createModelInstances(@RequestParam(name = "flags", required = false) Integer flags,
                                         @RequestParam(name = "fromImport", required = false, defaultValue = "false") boolean fromImport,
                                         @RequestBody List<CreateTopicDto> list) throws Exception {
        if (flags != null) {
            for (CreateTopicDto dto : list) {
                dto.setFlags(flags);
            }
        }
        Map<String, String> rs = unsAddService.createModelAndInstance(list,fromImport);
        if (rs == null || rs.isEmpty()) {
            return ResultVO.success("ok");
        }
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(206);
        resultVO.setData(rs);
        return resultVO;
    }

    @Operation(summary = "批量创建文件夹和文件(node-red导入专用)", tags = "openapi.tag.folder.management")
    @PostMapping(path = {"/inter-api/supos/uns/for/nodered"})
    public ResultVO createModelsForNodeRed(@RequestBody List<CreateUnsNodeRedDto> requestDto) throws Exception {
        List<String[]> results = unsAddService.createModelsForNodeRed(requestDto);
        return ResultVO.successWithData(results);
    }

    @Operation(summary = "删除前预先判断是否有被引用对象")
    @GetMapping(path = {"/inter-api/supos/uns/detectIfRemove"})
    public RemoveResult detect(@RequestParam(name = "id") @Parameter(description = "uns 主键") Long id) throws Exception {
        return unsRemoveService.detectRefers(id);
    }

    @Operation(summary = "删除指定路径下的所有文件夹和文件", tags = "openapi.tag.folder.management")
    @DeleteMapping({"/inter-api/supos/uns"})
    public RemoveResult removeModelOrInstance(@RequestParam(name = "id") @Parameter(description = "uns 主键") Long id
            , @RequestParam(name = "withFlow", defaultValue = "true") @Parameter(description = "是否删除相关流程") boolean withFlow
            , @RequestParam(name = "withDashboard", defaultValue = "true") @Parameter(description = "是否删除相关可视化面板") boolean withDashboard
            , @RequestParam(name = "cascade", required = false) @Parameter(description = "是否删除关联的文件") Boolean removeRefer
    ) throws Exception {
        RemoveResult rs = unsRemoveService.removeModelOrInstance(id, withFlow, withDashboard, removeRefer);
        log.info("删除uns: id={}, cascade={}, rs: {}", id, removeRefer, rs);
        return rs;
    }

    @Operation(summary = "根据别名集合批量删除文件夹和文件")
    @DeleteMapping({"/inter-api/supos/uns/batch/alias"})
    public ResponseEntity<RemoveResult> batchRemoveResultByAliasList(@RequestBody BatchRemoveUnsDto batchRemoveUnsDto) {
        return unsRemoveService.batchRemoveResultByAliasList(batchRemoveUnsDto);
    }

    @Operation(summary = "校验指定文件夹夹是否已存在文件夹、文件名称")
    @GetMapping("/inter-api/supos/uns/name/duplication")
    public ResultVO<Integer> checkDuplicationName(@RequestParam(name = "folder", required = false) @Parameter(description = "文件夹path") String folder
            , @RequestParam(name = "name") @Parameter(description = "待校验的文件夹、文件名称") String name
            , @RequestParam(name = "checkType") @Parameter(description = "校验类型：1--文件夹名称校验，2--文件名称校验") int checkType
    ) {
        return unsQueryService.checkDuplicationName(folder, name, checkType);
    }

//    @Deprecated
//    @Operation(summary = "从RestApi搜索模型字段")
//    @PostMapping("/inter-api/supos/uns/searchRestField")
//    public JsonResult<RestTestResponseVo> searchRestField(@RequestBody RestTestRequestVo requestVo) {
//        JsonResult<RestTestResponseVo> rs = null;
//        try {
//            return rs = unsQueryService.searchRestField(requestVo);
//        } catch (Exception ex) {
//            log.warn("searchRestFieldErr: " + ex.getMessage());
//            throw ex;
//        } finally {
//            log.info("searchRestField: body={}, res={}", requestVo, rs);
//        }
//    }

//    @Operation(summary = "手动触发RestApi")
//    @PostMapping("/inter-api/supos/uns/triggerRestApi")
//    public JsonResult<RestTestResponseVo> triggerRestApi(@RequestParam(name = "id", required = false) @Parameter(description = "文件对应的主题路径") Long fileId) {
//        return unsQueryService.triggerRestApi(fileId);
//    }

    @Operation(summary = "Mock rest")
    @GetMapping("/test/supos/uns/mockRest")
    public JsonResult<Map<String, Object>> mockRestPaged(@RequestParam(name = "name", required = false) List<String> names,
                                                         @RequestParam(name = "size", required = false, defaultValue = "3") int size) {
        if (names == null || names.isEmpty() || (names.size() == 1 && "[]".equals(names.get(0)))) {
            names = Arrays.asList("tag", "version");
        }
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<String, Object> data = new HashMap<>();
            for (String name : names) {
                if (name.contains("name") || name.contains("ver")) {
                    data.put(name, UUID.randomUUID().toString());
                } else {
                    data.put(name, new Random().nextInt(1000));
                }
            }
            list.add(data);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", list);

        log.info("mockRestPaged: names={}, res={}", names, res);

        return new JsonResult<>(0, "ok", res);
    }

    @GetMapping("/test/i18n")
    public String test18n(@RequestParam(name = "k") String k) {
        return I18nUtils.getMessage(k);
    }


    @Operation(summary = "批量查询文件实时值", tags = {"openapi.tag.folder.management"})
    @PostMapping(path = {"/inter-api/supos/uns/file/current/batchQuery"})
    public ResponseEntity<ResultVO> batchQueryFile(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "别名数组") List<String> aliasList) {
        return unsQueryService.batchQueryFile(aliasList);
    }

    @Operation(summary = "批量查询文件历史值", tags = {"openapi.tag.folder.management"})
    @PostMapping(path = {"/inter-api/supos/uns/file/history/batch/query"})
    public ResponseEntity<ResultVO<UnsHistoryQueryResult>> batchQueryFileHistoryValue(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "历史值查询请求参数") HistoryValueRequest historyValueRequest) {
        return unsDataService.batchQueryFileHistoryValue(historyValueRequest);
    }

    @Operation(summary = "批量写文件实时值",tags = {"openapi.tag.folder.management"})
    @PostMapping(path = {"/inter-api/supos/uns/file/current/batchUpdate"})
    public ResponseEntity<ResultVO<UnsDataResponseVo>> batchUpdateFile(@RequestBody List<UpdateFileDTO> list) {
        return ResponseEntity.ok(unsDataService.batchUpdateFile(list));
    }

    @Operation(summary = "外部topic payload解析" , tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/external/parserTopicPayload"})
    public ResultVO<List<OuterStructureVo>> parserTopicPayload(@RequestParam String topic) {
        return unsExternalTopicService.parserTopicPayload(topic);
    }

    @Operation(summary = "外部topic转UNS" , tags = "openapi.tag.folder.management")
    @PostMapping(path = {"/inter-api/supos/external/topic2Uns"})
    public ResultVO externalTopicAdd(@RequestBody CreateFileDto createFileDto) {
        return unsExternalTopicService.topic2Uns(createFileDto);
    }
}
