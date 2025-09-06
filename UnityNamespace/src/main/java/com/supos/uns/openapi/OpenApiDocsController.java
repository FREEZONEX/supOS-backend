package com.supos.uns.openapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@RestController
public class OpenApiDocsController {

    @GetMapping(value = "/inter-api/supos/supOS-CE.openapi.yaml", produces = "application/yaml")
    public String getOpenApiYaml() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("static/supOS-CE.openapi.yaml");
        if (inputStream == null) {
            throw new RuntimeException("File not found");
        }

        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next(); // 读取整个文件内容
        }
    }
}
