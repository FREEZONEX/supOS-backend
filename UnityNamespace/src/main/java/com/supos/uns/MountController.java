package com.supos.uns;

import com.supos.common.dto.BaseResult;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.mount.MountDto;
import com.supos.common.dto.mount.meta.common.CommonMountSourceDto;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.uns.service.mount.MountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 挂载
 * @date 2025/6/17 14:08
 */
@Slf4j
@RestController
public class MountController {

    @Autowired
    private MountService mountService;

    /**
     * 手动挂载
     * @param mountDto
     * @return
     */
    @PostMapping("/inter-api/supos/uns/mount")
    public BaseResult mount(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "挂载字段定义") @RequestBody MountDto mountDto) {
        mountService.mount(mountDto);
        return new BaseResult();
    }

    /**
     * 获取挂载数据源
     */
    @GetMapping("/inter-api/supos/uns/mount/source")
    public JsonResult<List<CommonMountSourceDto>> queryMountSource(@RequestParam(name = "sourceType", defaultValue = "collector") String sourceTypeValue) {
        MountSourceType sourceType = MountSourceType.getByType(sourceTypeValue);
        List<CommonMountSourceDto> sources = mountService.queryMountSource(sourceType);
        return new JsonResult<List<CommonMountSourceDto>>().setData(sources);
    }
}
