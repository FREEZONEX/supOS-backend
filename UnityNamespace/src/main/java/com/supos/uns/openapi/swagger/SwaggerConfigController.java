package com.supos.uns.openapi.swagger;

import cn.hutool.core.io.FileUtil;
import com.supos.common.Constants;
import com.supos.common.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class SwaggerConfigController {

    private final SwaggerUiManager swaggerUiManager;

    private static final String UNS_FILE = "/files/system/resource/swagger/supOS.yaml";
    private static final String UNS_FILE_EN = "/files/system/resource/swagger/supOS-en.yaml";

    // 存放用户上传的swagger文档列表
    public static final List<Map<String, String>> swaggerUrls = new ArrayList<>();

    // 初始化可以有一个默认的
    public SwaggerConfigController(SwaggerUiManager swaggerUiManager) {
        swaggerUrls.add(Map.of("name", "supOS-zh", "url", UNS_FILE));
        swaggerUrls.add(Map.of("name", "supOS-en", "url", UNS_FILE_EN));
        this.swaggerUiManager = swaggerUiManager;
    }

    @PostMapping("/api/swagger/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        // 校验文件后缀
        if (!file.getOriginalFilename().endsWith(".yaml") &&
            !file.getOriginalFilename().endsWith(".yml") &&
            !file.getOriginalFilename().endsWith(".json")) {
            return ResponseEntity.badRequest().body("只支持yaml/yml/json文件");
        }
        String originalFilename = file.getOriginalFilename();
        String extensionName = FileUtil.extName(originalFilename);
        String attachmentName = UUID.randomUUID().toString().replaceAll("-", "");
        if (StringUtils.isNotBlank(extensionName)) {
            attachmentName += "." + extensionName;
        }
        File uploadFile = destFile(attachmentName);
        //本地上传
        FileUtil.touch(uploadFile);
        try {
            file.transferTo(uploadFile);
        } catch (IOException ignored) {
        }
        // 添加到 swagger-ui 配置
        String url = "/files/uns/" + attachmentName;
//        swaggerUiManager.addUserSwagger(file.getOriginalFilename(), url);
        swaggerUiManager.addUserSwagger(url);
        swaggerUrls.add(Map.of("name", originalFilename, "url", url));
        return ResponseEntity.ok("上传成功: " + url);
    }

    // Swagger UI 会请求这个接口获取配置
    @GetMapping("/v3/api-docs/swagger-config")
    public Map<String, Object> swaggerConfig() {
        return Map.of(
                "urls", swaggerUrls,
                "validatorUrl", ""
        );
    }



    private static final File destFile(String fileName) {
        String targetPath = String.format("%s%s/%s", FileUtils.getFileRootPath(), Constants.UNS_ROOT, fileName);
        File outFile = FileUtil.touch(targetPath);
        return outFile;
    }
}
