package com.supos.uns;

import com.supos.common.dto.JsonResult;
import com.supos.common.dto.TopologyLog;
import com.supos.uns.bo.InstanceTopologyData;
import com.supos.uns.service.UnsTopologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 实例拓扑controller
 * @date 2024/12/20 9:18
 */
@RestController
@Slf4j
public class UnsTopologyController {

    @Autowired
    private UnsTopologyService unsTopologyService;

    @Operation(summary = "获取实例拓扑状态")
    @GetMapping(path = {"/inter-api/supos/uns/topology"}, produces = "application/json")
    public JsonResult<List<InstanceTopologyData>> instanceTopology(@RequestParam(name = "unsId", required = false) @Parameter(description = "文件unsId") Long unsId) throws Exception {
        List<InstanceTopologyData> topologyDatas = unsTopologyService.gainTopologyDataOfFile(unsId);

        return new JsonResult<>(0, "ok", topologyDatas);
    }

    @Operation(summary = "设置实例拓扑状态")
    @GetMapping(path = {"/inter-api/supos/uns/topology-mock"})
    public void mockInstanceTopology(@RequestParam(name = "unsId", required = false) @Parameter(description = "文件unsId") Long unsId,
            @RequestParam(name = "topic", required = false) @Parameter(description = "实例topic") String topic,
            @RequestParam(name = "node", required = false) @Parameter(description = "node") String node) throws Exception {
        TopologyLog.log(unsId, node, TopologyLog.EventCode.ERROR, "sd");
    }
}
