package com.supos.common.utils;

import cn.hutool.core.io.IoUtil;
import com.supos.common.Constants;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Slf4j
public class FileUtils {

    public static boolean deleteDir(File file) {
        if (!file.exists()) {
            return false;
        }
        Path path = file.toPath();
        boolean delDir = file.delete();
        if (!delDir) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                return false;
            }
            if (file.exists()) {
                delDir = file.delete();
            }
        }
        return delDir;
    }

    public static void downloadByPath(String path, HttpServletResponse response) {
        ClassPathResource classPathResource = new ClassPathResource(path);
        InputStream inputStream = null;
        try {
            inputStream = classPathResource.getInputStream();
            downloadFile(response, path.split("/")[path.split("/").length - 1], inputStream);
        } catch (IOException e) {
            log.error("downloadByPath Exception",e);
        } finally {
            IoUtil.close(inputStream);
        }
    }

    /**
     * 下载文件流
     *
     * @param response http response
     * @param fileName 文件名
     * @param inputStream 输入流
     * @throws IOException io
     */
    public static void downloadFile(HttpServletResponse response, String fileName, InputStream inputStream) {
        // 放到缓冲流里面
        BufferedInputStream bins = new BufferedInputStream(inputStream);
        // 获取文件输出IO流
        OutputStream outs = null;
        BufferedOutputStream bouts = null;
        try {
            outs = response.getOutputStream();
            bouts = new BufferedOutputStream(outs);
            response.setContentType("application/octet-stream;charset=UTF-8");
            fileName = new String(fileName.getBytes(), StandardCharsets.UTF_8);
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName,"UTF-8"));
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            // 传输文件流
            while ((bytesRead = bins.read(buffer, 0, 1024)) != -1) {
                bouts.write(buffer, 0, bytesRead);
            }
            bouts.flush();
        } catch (IOException e) {
            log.error("发生异常",e);
        } finally {
            IoUtil.close(bins);
            IoUtil.close(outs);
            IoUtil.close(bouts);
        }
    }

    /**
     * 返回挂载目录
     * @return 路径
     */
    public static String getFileRootPath() {
        return Constants.ROOT_PATH;
    }

    /**
     * 获取相对路径
     * @param absolutePath 绝对路径
     * @return 路径地址
     */
    public static String getRelativePath(String absolutePath){
        if (StringUtils.isNotBlank(absolutePath)){
            return absolutePath.replaceAll(getFileRootPath(),"");
        }
        return null;
    }
}
