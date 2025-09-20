package com.supos.i18n.controller;

import com.supos.common.dto.JsonResult;
import com.supos.common.utils.UserContext;
import com.supos.i18n.dto.I18nExportParam;
import com.supos.i18n.service.I18nExcelService;
import com.supos.i18n.dto.I18nExportRecordConfirmReq;
import com.supos.i18n.vo.I18nExportRecordVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化excel导入导出接口
 * @date 2025/9/1 14:05
 */
@RestController
@RequestMapping("/inter-api/supos/i18n/excel")
public class I18nExcelController {

    @Autowired
    private I18nExcelService i18nExcelService;

    /**
     * excel模板下载
     *
     * @param response
     */
    @GetMapping("/template/download")
    public void templateDownload(HttpServletResponse response) {
        i18nExcelService.doDownloadExcelTemplate(response);
    }

    /**
     * 数据导入
     *
     * @param file 文件
     * @return
     */
    @PostMapping("/template/import")
    public JsonResult<String> templateImport(@RequestParam("file") MultipartFile file) {
        return i18nExcelService.preImport(file);
    }

    /**
     * 数据导出
     *
     * @return
     */
    @PostMapping("/data/export")
    public JsonResult<String> dataExport(@RequestBody I18nExportParam i18nExportParam) {
        String sub = UserContext.get().getSub();
        i18nExportParam.setUserId(sub);
        return i18nExcelService.dataExport(i18nExportParam);
    }

    /**
     * 获取导出记录
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/data/getExportRecords")
    public JsonResult<List<I18nExportRecordVO>> getExportRecords(
            @RequestParam(value = "userId",required = false,defaultValue = "1") String userId,
            @RequestParam(value = "pageNo",required = false,defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize",required = false,defaultValue = "5") Integer pageSize) {
        String sub = UserContext.get().getSub();
        return i18nExcelService.getExportRecords(sub, pageNo, pageSize);
    }

    /**
     * 确认导出记录
     * @param req
     * @return
     */
    @PostMapping("/data/exportRecordConfirm")
    public JsonResult<String> exportRecordConfirm(@RequestBody I18nExportRecordConfirmReq req) {
        return i18nExcelService.exportRecordConfirm(req);
    }

    /**
     * 根据路径下载文件
     *
     * @param response
     * @param path
     */
    @GetMapping("/download")
    public void excelDownload(HttpServletResponse response, @RequestParam String path) {
        i18nExcelService.excelDownload(response, path);
    }

    @PostMapping("/template/test")
    public void templateImport() {
        i18nExcelService.asyncImport(new File("d:/i18n_zh_CN.xlsx"), (runningStatus) -> {},  false);
    }
}
