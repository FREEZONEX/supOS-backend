package com.supos.uns.openapi.swagger;

import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.stereotype.Component;

@Component
public class SwaggerUiManager {
    private final SwaggerUiConfigParameters swaggerUiConfigParameters;

    public SwaggerUiManager(SwaggerUiConfigParameters swaggerUiConfigParameters) {
        this.swaggerUiConfigParameters = swaggerUiConfigParameters;
    }

//    public void addUserSwagger(String name, String url) {
//        swaggerUiConfigParameters.addUrl(name, url);
//    }

    public void addUserSwagger(String url) {
        swaggerUiConfigParameters.addUrl(url);
    }
}
