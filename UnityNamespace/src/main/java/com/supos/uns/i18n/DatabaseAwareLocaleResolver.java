package com.supos.uns.i18n;

import cn.hutool.core.util.StrUtil;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import com.supos.uns.service.PersonConfigService;
import com.supos.uns.vo.PersonConfigVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * @author chenlizhong@supos.com
 * @description:
 * @date 2025/7/4 11:17
 */

public class DatabaseAwareLocaleResolver extends AcceptHeaderLocaleResolver {

    @Autowired
    private PersonConfigService personConfigService;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // session
        HttpSession session = request.getSession(false);
        if (session != null) {
            Locale locale = (Locale) session.getAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
            if (locale != null) {
                return locale;
            }
        }

        UserInfoVo userInfoVo = UserContext.get();

        if (userInfoVo != null) {
            PersonConfigVo configVo = personConfigService.getByUserId(userInfoVo.getSub());
            if (configVo != null && StrUtil.isNotBlank(configVo.getMainLanguage())) {

                Locale locale = Locale.forLanguageTag(configVo.getMainLanguage().replace("_", "-"));
                if (session != null) {
                    session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);
                }
                return locale;
            }
        }

        if (I18nUtils.SYS_OS_LANG != null) {
            Locale locale = Locale.forLanguageTag(I18nUtils.SYS_OS_LANG.replace("_", "-"));
            if (session != null) {
                session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);
            }
            return locale;
        }

        return super.resolveLocale(request);
    }
}
