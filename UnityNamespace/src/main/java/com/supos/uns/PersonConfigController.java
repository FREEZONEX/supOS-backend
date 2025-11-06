package com.supos.uns;

import cn.hutool.core.util.StrUtil;
import com.supos.common.exception.vo.ResultVO;
import com.supos.uns.service.PersonConfigService;
import com.supos.uns.vo.PersonConfigUpdateReqVo;
import com.supos.common.vo.PersonConfigVo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * @author chenlizhong@supos.com
 * @description:
 * @date 2025/7/3 14:02
 */
@Slf4j
@RestController
@RequestMapping("/inter-api/supos/uns/person")
public class PersonConfigController {

    @Autowired
    private PersonConfigService personConfigService;

    @Operation(summary = "获取个人配置")
    @GetMapping("/config")
    public ResultVO<PersonConfigVo> getPersonConfig(String userId) {
//        UserInfoVo userInfoVo = UserContext.get();
//        if (userInfoVo == null) {
//            return ResultVO.fail(I18nUtils.getMessage("user.not.login"));
//        }

        PersonConfigVo personConfigVo = personConfigService.getByUserId(userId);
        ResultVO resultVO = ResultVO.successWithData(personConfigVo);

        return resultVO;
    }

    @Operation(summary = "设置个人配置")
    @PostMapping("/config")
    public ResultVO update(@RequestBody PersonConfigUpdateReqVo req, HttpServletRequest request) throws Exception {
//        UserInfoVo userInfoVo = UserContext.get();
//        if (userInfoVo == null) {
//            return ResultVO.fail(I18nUtils.getMessage("user.not.login"));
//        }

        int ret = personConfigService.updateByUserId(req.getUserId(), req.getMainLanguage());

        if (ret > 0 && StrUtil.isNotBlank(req.getMainLanguage())) {
            // change req
            // set locale
            HttpSession session = request.getSession(false);
            if (session != null) {
                Locale locale = Locale.forLanguageTag(req.getMainLanguage().replace("_", "-"));
                session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);
            }
        }
        return ret > 0 ? ResultVO.success() : ResultVO.fail("system.error");
    }
    
}
