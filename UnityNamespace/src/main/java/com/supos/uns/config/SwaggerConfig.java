//package com.supos.uns.config;
//
//import com.supos.common.config.SystemConfig;
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Info;
//import io.swagger.v3.oas.models.info.License;
//import io.swagger.v3.oas.models.security.SecurityRequirement;
//import io.swagger.v3.oas.models.security.SecurityScheme;
//import io.swagger.v3.oas.models.servers.Server;
//import org.springdoc.core.models.GroupedOpenApi;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import jakarta.annotation.Resource;
//import java.util.Arrays;
//
//@Configuration
//public class SwaggerConfig {
//
//    @Resource
//    private SystemConfig systemConfig;
//
//    @Autowired
//    private CustomOperationCustomizer customOperationCustomizer;
//
//    @Bean
//    public OpenAPI openApi() {
//
//        // 定义 SecurityScheme（apikey 认证）
//        SecurityScheme apiKeyScheme = new SecurityScheme()
//                .type(SecurityScheme.Type.APIKEY) // APIKEY 类型
//                .name("apikey") // 请求头名称
//                .in(SecurityScheme.In.HEADER); // 指定在 Header 中传递
//
//
//        return new OpenAPI()
//                .servers(Arrays.asList(new Server().url(systemConfig.getEntranceUrl())))
//                .info(new Info().title("supOS-CE")
//                        .description("Open API Documentation")
//                        .version("v1.0.0")
//                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
//                .addSecurityItem(new SecurityRequirement().addList("apiKeyAuth"))
//                .components(new io.swagger.v3.oas.models.Components()
//                        .addSecuritySchemes("apiKeyAuth", apiKeyScheme)) // 注册 SecurityScheme
//                ;
//    }
//
//    @Bean
//    public GroupedOpenApi publicApi() {
//        return GroupedOpenApi.builder()
//                .group("supOS-openAPI")
//                .pathsToMatch("/open-api/**")  // 只暴露openAPI
//                .packagesToExclude("com.supos.pride")
//                .addOperationCustomizer(customOperationCustomizer)
//                .build();
//    }
//
//    @Bean
//    @ConditionalOnProperty(name = "DEBUG", havingValue = "true", matchIfMissing = false)
//    public GroupedOpenApi interApi() {
//        return GroupedOpenApi.builder()
//                .group("inter-api")
//                .pathsToMatch("/inter-api/supos/notify/**")
//                .build();
//    }
//
//}
