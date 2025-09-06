package com.supos.uns.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;

/**
 * @author chenlizhong@supos.com
 * @description:
 * @date 2025/6/27 13:55
 */
@Slf4j
@Configuration
public class I18nConfig {

    /**
     * 未配置时，默认简体中文
     */
    @Value("${SYS_OS_LANG:zh_CN}")
    private String systemLocale;


    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
//        SessionLocaleResolver sessionLocaleResolver = new SessionLocaleResolver();
//        // 从db中获取
//        Locale locale = Locale.forLanguageTag(systemLocale.replace("_", "-"));
//        // 设置默认区域
//        // 默认区域，可以从平台的全局配置中获取
//        sessionLocaleResolver.setDefaultLocale(locale);
//        return sessionLocaleResolver;

        DatabaseAwareLocaleResolver resolver = new DatabaseAwareLocaleResolver();

        return resolver;
    }

//    @Bean
//    public WebMvcConfigurer webMvcConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addInterceptors(InterceptorRegistry registry) {
//                LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
//                localeChangeInterceptor.setParamName("lang");
//                // 本地化拦截器需要放在其他拦截器之前
//                registry.addInterceptor(localeChangeInterceptor);
//            }
//        };
//    }
}
