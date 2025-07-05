package com.supos.uns;

import com.supos.common.Constants;
import com.supos.common.dto.JsonResult;
import com.supos.common.utils.DateTimeUtils;
import com.supos.uns.bo.PlugInfo;
import com.supos.uns.dto.PlugInfoDto;
import com.supos.uns.service.PluginManager;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;

@RestController
@Hidden
public class PluginController {

    @Autowired
    PluginManager pluginManager;

    /**
     * 列出已安装插件列表
     *
     * @return
     */
    @GetMapping("/inter-api/supos/plugin")
    public JsonResult<Collection<PlugInfo>> listPlugins() {
        return pluginManager.listPlugins();
    }

    @PostMapping("/inter-api/supos/plugin/install")
    public JsonResult<String> installPlugin(@RequestBody PlugInfoDto plugInfoDto) throws Exception {
        return pluginManager.installPlugin(plugInfoDto.getName());
    }

    @PostMapping("/inter-api/supos/plugin/upgrade")
    public JsonResult<String> upgradePlugin(@RequestParam("name") String name, @RequestParam("file") MultipartFile file) throws Exception {
        return pluginManager.upgradePlugin(name, file);
    }

    /**
     * 安装插件
     *
     * @param file 插件文件
     * @return
     */
    @PostMapping("/inter-api/supos/plugin/installTest")
    public JsonResult<String> installTest(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force// 是否卸载之前的包 强制安装新包
    ) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName.endsWith(".jar")) {
            byte[] data = StreamUtils.copyToByteArray(file.getInputStream());
            File jarFile = new File(Constants.PLUGIN_INSTALLED_PATH, DateTimeUtils.dateSimple() + "_" + fileName);
            jarFile.getParentFile().mkdirs();
            Files.write(jarFile.toPath(), data);
            return pluginManager.installPlugin(jarFile, null, force);
        } else {// TODO .zip 支持
            return new JsonResult<>(400, "Not a jar: " + fileName);
        }
    }

    /**
     * 卸载插件
     *
     * @param name - 插件名
     * @return
     * @throws Exception
     */
    @DeleteMapping("/inter-api/supos/plugin/uninstall")
    public JsonResult<String> uninstallPlugin(@RequestParam("name") String name) throws Exception {
        return pluginManager.uninstallPlugin(name);
    }

    @PostMapping("/inter-api/supos/plugin/{name}")
    public JsonResult<PlugInfo> pluginDetail(@PathVariable("name") String name) throws Exception {
        return new JsonResult<>(0, null, pluginManager.getPluginDetail(name));
    }

    @PostMapping(value = {"/inter-api/supos/plugin/uploadPlugin", "/open-api/supos/plugin/uploadPlugin"})
    public void uploadPlugin(@RequestBody PlugInfo plugInfo) throws Exception {
        pluginManager.putPlugin(plugInfo);
    }

    @DeleteMapping("/inter-api/supos/plugin/deletePlugin/{name}")
    public void deletePlugin(@PathVariable("name") String name) throws Exception {
        pluginManager.installPlugin(name);
    }

}
