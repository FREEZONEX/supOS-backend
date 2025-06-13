package com.supos.uns;

import com.supos.common.dto.BaseResult;
import com.supos.uns.service.UnsStreamService;
import com.supos.uns.vo.PaginationSearchResult;
import com.supos.uns.vo.StreamDetail;
import com.supos.uns.vo.StreamNewVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@RestController
@Slf4j
public class UnsStreamApiController {
    final UnsStreamService unsStreamService;

    public UnsStreamApiController(@Autowired UnsStreamService unsStreamService) {
        this.unsStreamService = unsStreamService;
    }

//    @Operation(summary = "创建流计算定义")
//    @PostMapping(path = "/inter-api/supos/uns/stream")
//    public BaseResult createStream(@RequestBody
//                                   @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "流计算定义")
//                                   StreamNewVo vo) {
//        return unsStreamService.createStream(vo);
//    }

    @Operation(summary = "分页查询流计算定义")
    @GetMapping(path = "/inter-api/supos/uns/stream")
    public PaginationSearchResult<List<StreamDetail>> listStreams(@RequestParam(name = "k", required = false) @Parameter(description = "模糊搜索词") String key,
                                                                  @RequestParam(name = "pageNo", required = false, defaultValue = "1") @Parameter(description = "页码，默认1") Integer pageNo,
                                                                  @RequestParam(name = "pageSize", required = false, defaultValue = "10") @Parameter(description = "每页条数，默认10") Integer pageSize
    ) {
        return unsStreamService.listStreams(key, pageNo, pageSize);
    }


    @Operation(summary = "删除流计算")
    @DeleteMapping("/inter-api/supos/uns/stream")
    @Valid
    public BaseResult deleteStream(@RequestParam("namespace") @NotEmpty String namespace) {
        return unsStreamService.deleteStream(namespace);
    }

    @Operation(summary = "停止流计算")
    @PutMapping("/inter-api/supos/uns/stream/stop")
    @Valid
    public BaseResult stopStream(@RequestParam("namespace") @NotEmpty String namespace) {
        return unsStreamService.stopStream(namespace);
    }

    @Operation(summary = "恢复流计算")
    @PutMapping("/inter-api/supos/uns/stream/resume")
    @Valid
    public BaseResult resumeStream(@RequestParam("namespace") @NotEmpty String namespace) {
        return unsStreamService.resumeStream(namespace);
    }
}
