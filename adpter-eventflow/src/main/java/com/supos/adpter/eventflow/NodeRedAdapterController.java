package com.supos.adpter.eventflow;

import com.supos.adpter.eventflow.service.NodeRedAdapterService;
import com.supos.adpter.eventflow.vo.*;
import com.supos.adpter.eventflow.vo.CopyFlowRequestVO;
import com.supos.adpter.eventflow.vo.CreateFlowRequestVO;
import com.supos.adpter.eventflow.vo.DeployFlowRequestVO;
import com.supos.adpter.eventflow.vo.NodeFlowVO;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.ResultDTO;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("EventFlowNodeRedAdapterController")
@Slf4j
public class NodeRedAdapterController {

    @Autowired
    private NodeRedAdapterService nodeRedAdapterService;


    /**
     * 创建新流程
     * @param requestBody
     * @return
     */
    @PostMapping("/inter-api/supos/event/flow")
    public ResultDTO createFlow(@Valid @RequestBody CreateFlowRequestVO requestBody) {
        long id = nodeRedAdapterService.createFlow(requestBody.getFlowName(), requestBody.getDescription(),requestBody.getTemplate());
        return ResultDTO.successWithData(id + "");
    }

    @PostMapping("/inter-api/supos/event/flow/copy")
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
    @GetMapping({"/inter-api/supos/event/flows"})
    @ResponseBody
    public PageResultDTO<NodeFlowVO> queryList(@Nullable @RequestParam("k") String fuzzyName,
                                               @RequestParam("pageNo") String pageNo,
                                               @RequestParam("pageSize") String pageSize) {
        return nodeRedAdapterService.selectList(fuzzyName, Integer.parseInt(pageNo), Integer.parseInt(pageSize));
    }

    /**
     * 部署单个流程, 流程ID从当前cookie中获取
     * @param requestBody
     * @return
     */
    @PostMapping("/inter-api/supos/event/flow/deploy")
    public ResultDTO deploy(@Valid @RequestBody DeployFlowRequestVO requestBody) {
        long id = Long.parseLong(requestBody.getId());
        String flowId = nodeRedAdapterService.proxyDeploy(id, requestBody.getFlows(), null);
        return ResultDTO.successWithData(flowId);
    }

    @PutMapping("/inter-api/supos/event/flow/save")
    public ResultDTO saveFlowJson(@Valid @RequestBody com.supos.adpter.eventflow.vo.SaveFlowJsonRequestVO requestBody) {
        long id = Long.parseLong(requestBody.getId());
        nodeRedAdapterService.saveFlowData(id, requestBody.getFlows());
        return ResultDTO.success("ok");
    }

    @GetMapping({"/inter-api/supos/event/flow/version"})
    public ResultDTO<String> getRevVersion() {
        String version = nodeRedAdapterService.getVersion();
        return ResultDTO.successWithData(version);
    }

    /**
     * 更新流程名称描述等
     * @param requestBody
     * @return
     */
    @PutMapping("/inter-api/supos/event/flow")
    public ResultDTO updateFlow(@Valid @RequestBody UpdateFlowRequestVO requestBody) {
        nodeRedAdapterService.updateFlow(requestBody);
        return ResultDTO.success("ok");
    }

    /**
     * 根据ID删除流程
     * @param id
     * @return
     */
    @DeleteMapping("/inter-api/supos/event/flow")
    public ResultDTO deleteFlow(@RequestParam("id") String id) {
        nodeRedAdapterService.deleteFlow(Long.parseLong(id), true);
        return ResultDTO.success("ok");
    }

}
