package com.supos.uns;

import com.supos.common.config.SystemConfig;
import com.supos.common.dto.SysModuleDto;
import com.supos.common.exception.vo.ResultVO;
import com.supos.uns.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置
 */
@Slf4j
@RestController
@Hidden
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 获取系统配置
     */
    @Operation(summary = "获取系统配置")
    @GetMapping(path = {"/inter-api/supos/systemConfig","/open-api/systemConfig"})
    public ResultVO<SystemConfig> systemConfig(){
        return systemConfigService.getSystemConfig();
    }
}
