package com.supos.uns;

import com.supos.common.dto.FileBlobDataQueryDto;
import com.supos.common.dto.UpdateFileDTO;
import com.supos.common.exception.vo.ResultVO;
import com.supos.uns.service.UnsDataService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 数据处理Controller
 * @date 2025/4/15 19:41
 */
@Hidden
@RestController
public class UnsDataController {

    @Autowired
    private UnsDataService unsDataService;

    @Operation(summary = "批量修改文件值", tags = "openapi.tag.folder.management")
    @PostMapping(path = {"/open-api/uns/file/batchUpdate"})
    public ResultVO batchUpdateFile(@RequestBody List<UpdateFileDTO> list) {
        return unsDataService.batchUpdateFile(list);
    }

    @Operation(summary = "批量查询文件实时值", tags = "openapi.tag.template.management")
    @GetMapping(path = {"/open-api/uns/file/batchQuery"})
    public ResultVO batchQueryFile(@RequestParam(name = "alias", required = false) @Parameter(description = "别名") List<String> alias,
                                   @RequestParam(name = "path", required = false) @Parameter(description = "文件路径") List<String> path
    ) {
        return unsDataService.batchQueryFile(alias, path);
    }

    @Operation(summary = "获取文件BLOB类型的值", tags = "openapi.tag.template.management")
    @PostMapping(path = {"/open-api/uns/file/blob"})
    public ResultVO batchQueryFile(@RequestBody FileBlobDataQueryDto query) {
        return ResultVO.successWithData(unsDataService.queryBlobValue(query));
    }
}
