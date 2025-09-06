package com.supos.uns;

import com.supos.common.dto.PageResultDTO;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.vo.LabelVo;
import com.supos.uns.openapi.dto.UpdateLabelDto;
import com.supos.uns.service.UnsLabelService;
import com.supos.uns.vo.FileVo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签管理
 */
@RestController
@Slf4j
@Hidden
public class UnsLabelApiController {

    @Autowired
    UnsLabelService unsLabelService;

    @Operation(summary = "标签列表", description = "列出所有支持的标签，下拉选择，支持模糊搜索", tags = "openapi.tag.label.management")
    @GetMapping(path = {"/inter-api/supos/uns/allLabel"})
    public ResultVO<List<LabelVo>> allLabels(@RequestParam(name = "key", required = false) @Parameter(description = "关键字") String key) {
        return unsLabelService.allLabels(key);
    }

    @Operation(summary = "标签详情", tags = "openapi.tag.label.management")
    @GetMapping(path = {"/inter-api/supos/uns/label/detail"})
    public ResultVO<LabelVo> labelDetail(@RequestParam(name = "id") @Parameter(description = "标签ID") Long id) {
        return unsLabelService.detail(id);
    }

    @Operation(summary = "创建标签", tags = "openapi.tag.label.management")
    @PostMapping(path = {"/inter-api/supos/uns/label"})
    public ResultVO createLabel(@RequestParam @Parameter(description = "标签名称") String name) {
        return unsLabelService.create(name);
    }

    @Operation(summary = "删除标签", tags = "openapi.tag.label.management")
    @DeleteMapping(path = {"/inter-api/supos/uns/label"})
    public ResultVO deleteLabel(@RequestParam @Parameter(description = "标签ID") Long id) {
        return unsLabelService.delete(id);
    }

    @Operation(summary = "修改标签", tags = "openapi.tag.label.management")
    @PutMapping(path = {"/inter-api/supos/uns/label"})
    public ResultVO updateLabel(@Valid @RequestBody UpdateLabelDto dto) {
        return unsLabelService.update(dto);
    }

    @Operation(summary = "文件打标签", tags = "openapi.tag.label.management")
    @PostMapping(path = {"/inter-api/supos/uns/makeLabel"})
    public ResultVO makeLabel(@RequestParam @Parameter(description = "文件ID") Long unsId,
                              @RequestBody(required = false) @Parameter(description = "标签集合，为空则取消所有标签") List<LabelVo> labelList) {
        return unsLabelService.makeLabel(unsId, labelList);
    }

    @Operation(summary = "文件打单个标签", tags = "openapi.tag.label.management")
    @PostMapping(path = {"/inter-api/supos/uns/makeSingleLabel"})
    public ResultVO makeSingleLabel(@RequestParam @Parameter(description = "文件ID") Long unsId,
                                    @RequestParam @Parameter(description = "标签ID")Long labelId) {
        return unsLabelService.makeSingleLabel(unsId, labelId);
    }

    @Operation(summary = "文件取消标签", tags = "openapi.tag.label.management")
    @DeleteMapping(path = {"/inter-api/supos/uns/cancelLabel"})
    public ResultVO cancelLabel(@RequestParam @Parameter(description = "文件ID") Long unsId,
                                @RequestBody @Parameter(description = "需要删除的标签ID列表") List<Long> labelIds) {
        return unsLabelService.cancelLabel(unsId, labelIds);
    }

    @Operation(summary = "分页获取标签下的文件列表", tags = "openapi.tag.label.management")
    @GetMapping(path = {"/inter-api/supos/uns/label/pageListUnsByLabel"})
    public PageResultDTO<FileVo> pageListUnsByLabel(@RequestParam(name = "labelId") Long labelId,
                                                    @RequestParam(defaultValue = "1", required = false) Long pageNo,
                                                    @RequestParam(defaultValue = "20", required = false) Long pageSize) {
        return unsLabelService.pageListUnsByLabel(labelId, pageNo, pageSize);
    }
}
