package com.supos.uns;

import com.supos.common.LogWrapperConsumer;
import com.supos.common.dto.JsonResult;
import com.supos.common.utils.UserContext;
import com.supos.uns.service.UnsExcelService;
import com.supos.uns.vo.ExportParam;
import com.supos.uns.vo.UnsExportRecordConfirmReq;
import com.supos.uns.vo.UnsExportRecordVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        String sub = UserContext.get().getSub();
        exportParam.setUserId(sub);
        return unsExcelService.dataExport(exportParam, exportParam.getAsync());
    }

    /**
     * 获取导出记录
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/data/getExportRecords")
    public JsonResult<List<UnsExportRecordVo>> getExportRecords(
            @RequestParam(value = "userId",required = false,defaultValue = "1") String userId,
            @RequestParam(value = "pageNo",required = false,defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize",required = false,defaultValue = "5") Integer pageSize) {
        String sub = UserContext.get().getSub();
        return unsExcelService.getExportRecords(sub, pageNo, pageSize);
    }

    /**
     * 确认导出记录
     * @param req
     * @return
     */
    @PostMapping("/data/exportRecordConfirm")
    public JsonResult<String> exportRecordConfirm(@RequestBody UnsExportRecordConfirmReq req) {
        return unsExcelService.exportRecordConfirm(req);
    }

    /**
     * 自测 UNS 导入本地文件
     *
     * @return
     */
    @PostMapping("/data/import/test")
    public void dataImport(@RequestParam("path") String path) {
        if (!StringUtils.hasText(path)) {
            //path = "./export/xx.json";
            path = "d:/all-namespace-category-zh-CN.xlsx";
            //path = "d:/all-namespace.json";
        }
        unsExcelService.asyncImport(new File(path), new LogWrapperConsumer(runningStatus -> {
        }), false, "zh");
    }
}
