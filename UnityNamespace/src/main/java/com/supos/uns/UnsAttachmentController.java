package com.supos.uns;

import com.supos.common.dto.BaseResult;
import com.supos.common.dto.JsonResult;
import com.supos.uns.bo.UnsAttachmentBo;
import com.supos.uns.service.UnsAttachmentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/8 15:10
 */
@RestController
@Slf4j
public class UnsAttachmentController {

    @Resource
    private UnsAttachmentService unsAttachmentService;


    /**
     * 模板实例附件上传
     *
     * @param files 附件文件
     * @return
     */
    @PostMapping("/inter-api/supos/uns/attachment")
    public BaseResult attachmentUpload(@RequestParam("alias") String alias, @RequestParam("files") MultipartFile[] files) {
        String err = unsAttachmentService.upload(alias, files);
        if (err != null) {
            return new BaseResult(400, err);
        }
        return new BaseResult();
    }

    /**
     * 模板实例附件下载
     *
     * @param objectName 附件名
     * @return
     */
    @GetMapping("/inter-api/supos/uns/attachment")
    public void attachmentDownload(@RequestParam("objectName") String objectName, HttpServletResponse response) {
        try {
            Pair<String, InputStream> objectPiar = unsAttachmentService.download(objectName);
            response.setHeader("Content-Disposition", "attachment;filename=" + UriUtils.encode(objectPiar.getLeft(), "UTF-8"));
            response.setContentType("application/force-download");
            response.setCharacterEncoding("UTF-8");
            IOUtils.copy(objectPiar.getRight(), response.getOutputStream());
        } catch (Exception e) {
            log.error("下载失败", e);
        }
    }

    /**
     * 模板实例附件预览
     *
     * @param objectName 附件名
     * @return
     */
    @GetMapping("/inter-api/supos/uns/attachment/preview")
    public void attachmentPreview(@RequestParam("objectName") String objectName, HttpServletResponse response) {
        try {
            Pair<String, InputStream> objectPiar = unsAttachmentService.download(objectName);
/*            response.setHeader("Content-Disposition", "attachment;filename=" + UriUtils.encode(objectPiar.getLeft(), "UTF-8"));
            response.setContentType("application/force-download");*/
            response.setCharacterEncoding("UTF-8");
            IOUtils.copy(objectPiar.getRight(), response.getOutputStream());
        } catch (Exception e) {
            log.error("下载失败", e);
        }
    }

    /**
     * 模板实例附件删除
     *
     * @param objectName 附件名
     * @return
     */
    @DeleteMapping("/inter-api/supos/uns/attachment")
    public BaseResult attachmentDelete(@RequestParam("objectName") String objectName) {
        unsAttachmentService.delete(objectName);
        return new BaseResult();
    }

    /**
     * 获取模板实例附件列表
     *
     * @param alias 模板实例别名
     * @return
     */
    @GetMapping("/inter-api/supos/uns/attachments")
    public JsonResult<List<UnsAttachmentBo>> listAttachment(@RequestParam("alias") String alias) {
        List<UnsAttachmentBo> list = unsAttachmentService.query(alias);
        return new JsonResult<>(0, "ok", list);
    }
}
