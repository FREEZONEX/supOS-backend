package com.supos.adpter.kong;

import com.supos.adpter.kong.service.MenuService;
import com.supos.adpter.kong.validator.OpenTypeValidator;
import com.supos.common.annotation.MenuNameValidator;
import com.supos.common.annotation.ServiceNameValidator;
import com.supos.adpter.kong.dto.MenuDto;
import com.supos.common.exception.vo.ResultVO;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 菜单Controller
 * @date 2025/4/16 18:55
 */
@Slf4j
@Validated
@RestController
@Hidden
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 保存菜单
     * @param name 编号（唯一）
     * @param description 描述
     * @param baseUrl baseURL
     * @param openType 跳转方式
     * @param icon 菜单icon
     * @return
     */
    @Operation(summary = "保存菜单",tags = "openapi.tag.menu.management")
    @PostMapping("/open-api/menu")
    public ResultVO saveMenu(@Parameter(description = "服务名", example = "serviceName") @ServiceNameValidator @RequestParam(name = "serviceName", required = false) String serviceName,
                             @Parameter(description = "名称，唯一标识一个菜单", example = "name") @MenuNameValidator @NotBlank(message = "menu.name.null") @RequestParam(name = "name") String name,
                             @Parameter(description = "显示名",example = "showName") @NotBlank(message = "menu.showname.null") @Size(max = 64, message = "menu.showname.length") @RequestParam(name = "showName") String showName,
                             @Parameter(description = "描述", example = "description") @Size(max = 512, message = "menu.description.length") @RequestParam(name = "description", required = false) String description,
                             @Parameter(description = "baseUrl", example = "http://www.supos.com") @Size(max = 1024, message = "menu.baseurl.length")@RequestParam(name = "baseUrl") String baseUrl,
                             @Parameter(description = "跳转方式tag，0:iframe打开  1:打开新页面", example = "0") @OpenTypeValidator @RequestParam(name = "openType") Integer openType,
                             @Parameter(description = "icon文件") @RequestParam(name = "file", required = false) MultipartFile icon) {
        MenuDto menuDto = new MenuDto();
        menuDto.setServiceName(serviceName);
        menuDto.setName(name);
        menuDto.setShowName(showName);
        menuDto.setDescription(description);
        menuDto.setBaseUrl(baseUrl);
        menuDto.setOpenType(openType);
        menuDto.setIcon(icon);
        menuService.createMenu(menuDto, true);
        return ResultVO.success();
    }

}
