package com.supos.uns;

import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.nodered.vo.MarkTopRequestVO;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.PaginationDTO;
import com.supos.common.dto.ResultDTO;
import com.supos.common.dto.grafana.DashboardDto;
import com.supos.common.dto.grafana.DashboardRefDto;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.uns.dao.po.DashboardPo;
import com.supos.uns.service.DashboardService;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inter-api/supos/uns/dashboard")
@Slf4j
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/detail")
    public ResultVO getDashboardDetail(@RequestParam String id){
        return ResultVO.successWithData(dashboardService.getById(id));
    }

    @GetMapping
    public PageResultDTO<DashboardDto> pageList(@Nullable @RequestParam("k") String keyword,
                                                @RequestParam(value = "type",required = false) Integer type,
                                                @Nullable @RequestParam("orderCode") String orderCode,
                                                @Nullable @RequestParam("isAsc") String isAsc,
                                                PaginationDTO params){
        String descOrAsc = "true".equals(isAsc) ? "ASC" : "DESC";
        if (StringUtils.isNotEmpty(orderCode)) {
            if (!"name".equals(orderCode) && !"createTime".equals(orderCode)) {
                throw new BuzException("illegal sort param");
            }
            orderCode = orderCode.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        }
        return dashboardService.pageList(keyword,type, orderCode, descOrAsc, params);
    }

    @PostMapping
    public JsonResult<DashboardPo> create(@RequestBody DashboardDto dashboardDto){
        return dashboardService.create(dashboardDto);
    }


    @PutMapping
    public JsonResult edit(@RequestBody DashboardDto dashboardDto){
        dashboardService.edit(dashboardDto);
        return new JsonResult<>();
    }


    @DeleteMapping("/{uid}")
    public JsonResult delete(@PathVariable String uid){
        dashboardService.delete(uid);
        return new JsonResult<>();
    }

    @GetMapping("/{uid}")
    public ResultVO<JSONObject> getByUuid(@PathVariable String uid){
        return dashboardService.getByUuid(uid);
    }


    @PostMapping("/createGrafanaByUns/{alias}")
    public ResultVO createGrafanaByUns(@PathVariable String alias){
        return dashboardService.createGrafanaByUns(alias);
    }


    @GetMapping("/isExist")
    public ResultVO isExist(@RequestParam String alias){
        return dashboardService.isExist(alias);
    }


    /**
     * 置顶
     * @param markRequest
     * @return
     */
    @PostMapping("/mark")
    public ResultDTO markTop(@Valid @RequestBody MarkTopRequestVO markRequest) {
        dashboardService.markTop(markRequest.getId());
        return ResultDTO.success("ok");
    }

    /**
     * 取消置顶
     * @param id
     * @return
     */
    @DeleteMapping("/unmark")
    public ResultDTO removeMarked(@RequestParam("id") String id) {
        dashboardService.removeMarkedTop(id);
        return ResultDTO.success("ok");
    }

    @PostMapping("/bindUns")
    public ResultVO bindUns(@RequestBody DashboardRefDto dto){
        return dashboardService.bindUns(dto);
    }

    @GetMapping("/getByUns")
    public ResultVO<DashboardPo> getByUns(@RequestParam String unsAlias){
        return dashboardService.getByUns(unsAlias);
    }

}
