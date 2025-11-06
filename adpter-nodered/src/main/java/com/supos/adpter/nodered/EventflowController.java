package com.supos.adpter.nodered;

import com.supos.adpter.nodered.enums.FlowType;
import com.supos.adpter.nodered.service.EventflowAdapterService;
import com.supos.adpter.nodered.vo.*;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.ResultDTO;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("EventFlowNodeRedAdapterController")
@Slf4j
public class EventflowController {

    @Autowired
    private EventflowAdapterService eventflowAdapterService;
    /**
     * 创建新流程
     * @param requestBody
     * @return
     */
    @PostMapping("/inter-api/supos/event/flow")
    public ResultDTO createFlow(@Valid @RequestBody CreateFlowRequestVO requestBody) {
        long id = eventflowAdapterService.createFlow(requestBody.getFlowName(), requestBody.getDescription(), FlowType.EVENTFLOW.getFlowName());
        return ResultDTO.successWithData(id + "");
    }

    @PostMapping("/inter-api/supos/event/flow/copy")
    public ResultDTO copyFlow(@Valid @RequestBody CopyFlowRequestVO requestBody) {
        long id = eventflowAdapterService.copyFlow(
                requestBody.getSourceId(),
                requestBody.getFlowName(),
                requestBody.getDescription(),
                FlowType.EVENTFLOW.getFlowName());
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
                                               @Nullable @RequestParam("orderCode") String orderCode,
                                               @Nullable @RequestParam("isAsc") String isAsc,
                                               @RequestParam("pageNo") String pageNo,
                                               @RequestParam("pageSize") String pageSize) {
        String descOrAsc = "true".equals(isAsc) ? "ASC" : "DESC";
        if (StringUtils.isNotEmpty(orderCode)) {
            orderCode = orderCode.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        }
        return eventflowAdapterService.selectList(fuzzyName,  FlowType.EVENTFLOW.getFlowName(),
                orderCode,
                descOrAsc,
                Integer.parseInt(pageNo),
                Integer.parseInt(pageSize));
    }

    /**
     * 部署单个流程, 流程ID从当前cookie中获取
     * @param requestBody
     * @return
     */
    @PostMapping("/inter-api/supos/event/flow/deploy")
    public ResultDTO deploy(@Valid @RequestBody DeployFlowRequestVO requestBody) {
        long id = Long.parseLong(requestBody.getId());
        DeployResponseVO deployResponse = eventflowAdapterService.proxyDeploy(id, requestBody.getFlows(), null);
        return ResultDTO.successWithData(deployResponse);
    }

    @PutMapping("/inter-api/supos/event/flow/save")
    public ResultDTO saveFlowJson(@Valid @RequestBody SaveFlowJsonRequestVO requestBody) {
        long id = Long.parseLong(requestBody.getId());
        eventflowAdapterService.saveFlowData(id, requestBody.getFlows());
        return ResultDTO.success("ok");
    }

    @GetMapping({"/inter-api/supos/event/flow/version"})
    public ResultDTO<String> getRevVersion() {
        String version = eventflowAdapterService.getVersion();
        return ResultDTO.successWithData(version);
    }

    /**
     * 更新流程名称描述等
     * @param requestBody
     * @return
     */
    @PutMapping("/inter-api/supos/event/flow")
    public ResultDTO updateFlow(@Valid @RequestBody UpdateFlowRequestVO requestBody) {
        eventflowAdapterService.updateFlow(requestBody);
        return ResultDTO.success("ok");
    }

    /**
     * 根据ID删除流程
     * @param id
     * @return
     */
    @DeleteMapping("/inter-api/supos/event/flow")
    public ResultDTO deleteFlow(@RequestParam("id") String id) {
        eventflowAdapterService.deleteFlow(Long.parseLong(id), true);
        return ResultDTO.success("ok");
    }

    /**
     * 置顶
     * @param markRequest
     * @return
     */
    @PostMapping("/inter-api/supos/event/mark")
    public ResultDTO markTop(@Valid @RequestBody MarkTopRequestVO markRequest) {
        eventflowAdapterService.markTop(Long.parseLong(markRequest.getId()));
        return ResultDTO.success("ok");
    }

    /**
     * 取消置顶
     * @param id
     * @return
     */
    @DeleteMapping("/inter-api/supos/event/unmark")
    public ResultDTO removeMarked(@RequestParam("id") String id) {
        eventflowAdapterService.removeMarkedTop(Long.parseLong(id));
        return ResultDTO.success("ok");
    }
}
