package com.supos.uns.openapi;

import com.supos.adpter.kong.vo.ResultVO;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.openapi.dto.UserPageQueryDto;
import com.supos.uns.openapi.service.UserOpenapiService;
import com.supos.uns.openapi.vo.UserDetailVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class UserOpenApi {

    @Autowired
    private UserOpenapiService userOpenapiService;

    @Operation(summary = "用户列表",tags = "openapi.tag.user.management")
    @GetMapping({"/open-api/user/pageList"})
    public PageResultDTO<UserDetailVo> openUserPageList(
            @Validated @ParameterObject UserPageQueryDto params){
        return userOpenapiService.userManageList(params);
    }


    @Operation(summary = "用户详情",tags = "openapi.tag.user.management")
    @GetMapping({"/open-api/user/{username}"})
    public ResultVO<UserDetailVo> userDetail(@PathVariable @Parameter(description = "用户名") String username){
        UserPageQueryDto queryDto = new UserPageQueryDto();
        queryDto.setUsername(username);
        PageResultDTO<UserDetailVo> resultDTO = userOpenapiService.userManageList(queryDto);
        if (CollectionUtils.isEmpty(resultDTO.getData())){
            ResultVO resultVO = ResultVO.fail(I18nUtils.getMessage("user.not.exist"));
            resultVO.setCode(404);
            return resultVO;
        }
        return ResultVO.success(resultDTO.getData().get(0));
    }

}
