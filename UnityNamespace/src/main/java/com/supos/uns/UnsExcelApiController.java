package com.supos.uns;

import com.supos.common.dto.JsonResult;
import com.supos.uns.service.UnsExcelService;
import com.supos.uns.vo.ExportParam;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/inter-api/supos/uns/excel")
@Slf4j
public class UnsExcelApiController {

    @Resource
    private UnsExcelService unsExcelService;

    @GetMapping(path = "/test", produces = "text/html")
    public String test() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ws_upload_excel.htm");
        return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * 模板数据导入
     *
     * @param file 模板文件
     * @return
     */
    @PostMapping("/template/import")
    public JsonResult<String> templateImport(@RequestParam("file") MultipartFile file) {
        return unsExcelService.templateImport(file);
    }

    /**
     * excel模板下载
     *
     * @param response
     */
    @GetMapping("/template/download")
    public void templateDownload(@RequestParam("fileType") String fileType, HttpServletResponse response) {
        unsExcelService.downloadTemplate(fileType, response);
    }

    /**
     * 根据路径下载文件
     *
     * @param response
     * @param path
     */
    @GetMapping("/download")
    public void excelDownload(HttpServletResponse response, @RequestParam String path) {
        unsExcelService.excelDownload(response, path);
    }

    /**
     * 数据导出
     *
     * @return
     */
    @PostMapping("/data/export")
    public JsonResult<String> dataExport(@RequestBody ExportParam exportParam) {
        return unsExcelService.dataExport(exportParam);
    }

    /**
     * 自测 UNS 导入本地文件
     *
     * @return
     */
    @PostMapping("/data/import/test")
    public void dataImport(@RequestParam("path") String path) {
        unsExcelService.asyncImport(new File(path), new UnsExcelService.LogWrapperConsumer(runningStatus -> {
        }), false);
    }
}
