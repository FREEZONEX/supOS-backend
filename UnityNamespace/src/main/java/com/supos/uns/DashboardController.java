package com.supos.uns;

import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.PaginationDTO;
import com.supos.common.dto.grafana.DashboardDto;
import com.supos.common.exception.vo.ResultVO;
import com.supos.uns.dao.po.DashboardPo;
import com.supos.uns.service.DashboardService;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
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
    public PageResultDTO<DashboardDto> pageList(@Nullable @RequestParam("k") String keyword,@RequestParam(value = "type",required = false) Integer type, PaginationDTO params){
        return dashboardService.pageList(keyword,type, params);
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


}
