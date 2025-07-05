package com.supos.adpter.nodered;

import com.supos.adpter.nodered.service.ImportNodeRedFlowService;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.ResultDTO;
import com.supos.adpter.nodered.service.NodeRedAdapterService;
import com.supos.adpter.nodered.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@Slf4j
public class NodeRedAdapterController {

    @Autowired
    private NodeRedAdapterService nodeRedAdapterService;
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
                                               @RequestParam("pageNo") String pageNo,
                                               @RequestParam("pageSize") String pageSize) {
        return nodeRedAdapterService.selectList(fuzzyName, Integer.parseInt(pageNo), Integer.parseInt(pageSize));
    }

    @GetMapping({"/inter-api/supos/flow/uns/alias"})
    public ResultDTO<NodeFlowVO> getByAlias(@RequestParam("alias") String alias) {
        NodeFlowVO result = nodeRedAdapterService.getByAlias(alias);
        return ResultDTO.successWithData(result);
    }

    @PostMapping("/inter-api/supos/flow/create")
    public ResultDTO<NodeFlowVO> createMockFlow(@Valid @RequestBody CreateMockFlowRequestVO requestBody) {
        NodeFlowVO mockFlow = importNodeRedFlowService.createMockFlow(requestBody.getPath(), requestBody.getUnsAlias());
        return ResultDTO.successWithData(mockFlow);
    }

    /**
     * 部署单个流程, 流程ID从当前cookie中获取
     * @param requestBody
     * @return
     */
    @PostMapping("/inter-api/supos/flow/deploy")
    public ResultDTO deploy(@Valid @RequestBody DeployFlowRequestVO requestBody) {
        long id = Long.parseLong(requestBody.getId());
        String flowId = nodeRedAdapterService.proxyDeploy(id, requestBody.getFlows(), null);
        return ResultDTO.successWithData(flowId);
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

}
