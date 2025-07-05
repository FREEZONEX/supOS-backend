package com.supos.uns;

import com.supos.common.dto.JsonResult;
import com.supos.uns.service.GlobalExportService;
import com.supos.uns.vo.ExportRecordConfirmReq;
import com.supos.uns.vo.ExportRecordVo;
import com.supos.uns.vo.GlobalExportParam;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月19日 10:40
 */
@Slf4j
@RestController
@RequestMapping("/inter-api/supos/global")
public class GlobalExportController {
    @Autowired
    private GlobalExportService globalExportService;

    /**
     * 全局数据导入
     *
     * @param file zip文件
     * @return
     */
    @PostMapping("/data/import")
    public JsonResult<String> dataImport(@RequestParam("file") MultipartFile file) {
        return globalExportService.dataImport(file);
    }

    /**
     * 全局数据导出
     *
     * @return
     */
    @PostMapping("/data/export")
    public JsonResult<String> dataExport(@RequestBody GlobalExportParam globalExportParam) {
        return globalExportService.dataExport(globalExportParam);
    }

    /**
     * 获取导出记录
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/user/getExportRecords")
    public JsonResult<List<ExportRecordVo>> getExportRecords(@RequestParam(value = "pageNo",required = false,defaultValue = "1") Integer pageNo,
                                                            @RequestParam(value = "pageSize",required = false,defaultValue = "5") Integer pageSize) {
        return globalExportService.getExportRecords(pageNo, pageSize);
    }

    /**
     * 确认导出记录
     * @param req
     * @return
     */
    @PostMapping("/user/exportRecordConfirm")
    public JsonResult<String> exportRecordConfirm(@RequestBody ExportRecordConfirmReq req) {
        return globalExportService.exportRecordConfirm(req);
    }


    /**
     * 根据路径下载文件
     *
     * @param response
     * @param path
     */
    @GetMapping("/file/download")
    public void excelDownload(HttpServletResponse response, @RequestParam String path) {
        globalExportService.fileDownload(response, path);
    }
}
