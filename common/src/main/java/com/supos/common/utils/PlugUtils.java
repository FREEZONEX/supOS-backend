package com.supos.common.utils;

import com.supos.common.dto.PlugInfoYml;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/5/20 19:01
 */
@Slf4j
public class PlugUtils {

    /**
     * 解压缩插件包
     *
     * @param plugPackage
     * @param plugPackagePath
     */
    public static void unzipPlugin(File plugPackage, String plugPackagePath) {
        File outputDirFile = new File(plugPackagePath);
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs();
        }

        try (InputStream fi = new FileInputStream(plugPackage);
             InputStream bi = new BufferedInputStream(fi);
             InputStream gzi = new GzipCompressorInputStream(bi);
             TarArchiveInputStream ti = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            while ((entry = ti.getNextTarEntry()) != null) {
                File outputFile = new File(plugPackagePath, entry.getName());

                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                } else {
                    File parent = outputFile.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }

                    try (OutputStream outputFileStream = new FileOutputStream(outputFile)) {
                        IOUtils.copy(ti, outputFileStream);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解压插件包失败", e);
        }
    }

    /**
     * 获取plug.yml内容
     *
     * @param plugPackagePath
     * @return
     */
    public static PlugInfoYml getPlugInfoYml(String plugPackagePath) {
        return MyYamlUtils.loadYaml(plugPackagePath + File.separator + "plug.yml", PlugInfoYml.class);
    }

}
