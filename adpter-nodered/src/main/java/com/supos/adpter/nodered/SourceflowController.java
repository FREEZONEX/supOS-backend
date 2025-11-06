package com.supos.adpter.nodered;

import com.supos.adpter.nodered.dto.ExportNodeFlowDto;
import com.supos.adpter.nodered.enums.FlowType;
import com.supos.adpter.nodered.service.ImportNodeRedFlowService;
import com.supos.adpter.nodered.service.SourceFlowApiService;
import com.supos.adpter.nodered.service.SourceflowAdapterService;
import com.supos.adpter.nodered.vo.*;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.ResultDTO;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class SourceflowController {

    @Autowired
    private SourceflowAdapterService nodeRedAdapterService;
    @Autowired
    private ImportNodeRedFlowService importNodeRedFlowService;

    /**
     * 创建新流程
     * @param requestBody
     * @return
     */
    @PostMapping("/inter-api/supos/flow")
    public ResultDTO createFlow(@Valid @RequestBody CreateFlowRequestVO requestBody) {
        long id = nodeRedAdapterService.createFlow(requestBody.getFlowName(), requestBody.getDescription(),requestBody.getTemplate());
        return ResultDTO.successWithData(id + "");
    }

    @PostMapping("/inter-api/supos/flow/copy")
    public ResultDTO copyFlow(@Valid @RequestBody CopyFlowRequestVO requestBody) {
        long id = nodeRedAdapterService.copyFlow(
                requestBody.getSourceId(),
                requestBody.getFlowName(),
                requestBody.getDescription(),
                requestBody.getTemplate());
        return ResultDTO.successWithData(id + "");
    }

    /**
     * 查询流程列表，支持分页和模糊搜索
     * @param fuzzyName 流程名称 模糊查询
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping({"/inter-api/supos/flows"})
    @ResponseBody
    public PageResultDTO<NodeFlowVO> queryList(@Nullable @RequestParam("k") String fuzzyName,
                                               @Nullable @RequestParam("orderCode") String orderCode,
                                               @Nullable @RequestParam("isAsc") String isAsc,
                                               @RequestParam("pageNo") String pageNo,
                                               @RequestParam("pageSize") String pageSize) {
        String descOrAsc = "true".equals(isAsc) ? "ASC" : "DESC";
        if (StringUtils.isNotEmpty(orderCode)) {
            if (!"flowName".equals(orderCode) && !"createTime".equals(orderCode)) {
                throw new BuzException("illegal sort param");
            }
            orderCode = orderCode.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        }
        return nodeRedAdapterService.selectList(fuzzyName,  FlowType.NODERED.getFlowName(),
                orderCode,
                descOrAsc,
                Integer.parseInt(pageNo),
                Integer.parseInt(pageSize));
    }

    @GetMapping({"/inter-api/supos/flow/uns/alias"})
    public ResultDTO<NodeFlowVO> getByAlias(@RequestParam("alias") String alias) {
        NodeFlowVO result = nodeRedAdapterService.getByAlias(alias);
        return ResultDTO.successWithData(result);
    }

    @Autowired
    private SourceFlowApiService sourceFlowApiService;

    @PostMapping({"/inter-api/supos/flow/export/alias"})
    public ResultDTO exportByAlias(@Valid @RequestBody ExportNodeFlowDto requestBody) {
        sourceFlowApiService.importFlow(requestBody);
        return ResultDTO.success("");
    }

    @PostMapping("/inter-api/supos/flow/create")
    public ResultDTO<NodeFlowVO> createMockFlow(@Valid @RequestBody CreateMockFlowRequestVO requestBody) {
        NodeFlowVO mockFlow = importNodeRedFlowService.createMockFlow(requestBody.getPath(), requestBody.getUnsAlias());
        return ResultDTO.successWithData(mockFlow);
    }

    @GetMapping({"/inter-api/supos/flow/version"})
    public ResultDTO<String> getRevVersion() {
        String version = nodeRedAdapterService.getVersion();
        return ResultDTO.successWithData(version);
    }

    /**
     * 部署单个流程, 流程ID从当前cookie中获取
     * @param requestBody
     * @return
     */
    @PostMapping("/inter-api/supos/flow/deploy")
    public ResultDTO deploy(@Valid @RequestBody DeployFlowRequestVO requestBody) {
        long id = Long.parseLong(requestBody.getId());
        DeployResponseVO deployResponse = nodeRedAdapterService.proxyDeploy(id, requestBody.getFlows(), null);
        return ResultDTO.successWithData(deployResponse);
    }

    @PutMapping("/inter-api/supos/flow/save")
    public ResultDTO saveFlowJson(@Valid @RequestBody SaveFlowJsonRequestVO requestBody) {
        long id = Long.parseLong(requestBody.getId());
        nodeRedAdapterService.saveFlowData(id, requestBody.getFlows());
        return ResultDTO.success("ok");
    }

    /**
     * 更新流程名称描述等
     * @param requestBody
     * @return
     */
    @PutMapping("/inter-api/supos/flow")
    public ResultDTO updateFlow(@Valid @RequestBody UpdateFlowRequestVO requestBody) {
        nodeRedAdapterService.updateFlow(requestBody);
        return ResultDTO.success("ok");
    }

    /**
     * 根据ID删除流程
     * @param id
     * @return
     */
    @DeleteMapping("/inter-api/supos/flow")
    public ResultDTO deleteFlow(@RequestParam("id") String id) {
        nodeRedAdapterService.deleteFlow(Long.parseLong(id), true);
        return ResultDTO.success("ok");
    }

    @PostMapping("/inter-api/supos/flow/mark")
    public ResultDTO markTop(@Valid @RequestBody MarkTopRequestVO markRequest) {
        nodeRedAdapterService.markTop(Long.parseLong(markRequest.getId()));
        return ResultDTO.success("ok");
    }

    @DeleteMapping("/inter-api/supos/flow/unmark")
    public ResultDTO removeMarked(@RequestParam("id") String id) {
        nodeRedAdapterService.removeMarkedTop(Long.parseLong(id));
        return ResultDTO.success("ok");
    }

    @PostMapping("/inter-api/supos/flow/bindUns")
    public ResultVO bindUns(@RequestParam Long flowId, @RequestParam String unsAlias){
        return nodeRedAdapterService.bindUns(flowId, unsAlias);
    }

}
