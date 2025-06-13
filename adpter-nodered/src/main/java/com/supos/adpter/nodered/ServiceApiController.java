package com.supos.adpter.nodered;

import com.supos.adpter.nodered.service.NodeRedAdapterService;
import com.supos.adpter.nodered.vo.BatchQueryRequest;
import com.supos.adpter.nodered.vo.NodeFlowVO;
import com.supos.common.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class ServiceApiController {

    @Autowired
    private NodeRedAdapterService nodeRedAdapterService;


    @PostMapping({"/service-api/supos/flow/by/aliases"})
    public ResultDTO<List<NodeFlowVO>> batchQueryByAlias(@RequestBody BatchQueryRequest batchQueryRequest) {
        List<NodeFlowVO> result = nodeRedAdapterService.selectByAliases(batchQueryRequest.getAliases());
        if (result.isEmpty()) {
            return ResultDTO.successWithData(null);
        }
        return ResultDTO.successWithData(result);
    }


}
