package com.supos.uns.bo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.annotations.JsonAdapter;
import com.supos.common.dto.PlugInfoYml;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.config.ToStringSerializer;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Getter
@Setter
public class PlugInfo {
    public static final String STATUS_INSTALLED = "installed";
    public static final String STATUS_NOT_INSTALL = "notInstall";
    public static final String STATUS_INSTALL_FAIL = "installFail";


    /**
     * 插件元数据，对应插件的plug.yml文件
     */
    @Schema(description = "插件元数据信息")
    private PlugInfoYml plugInfoYml;

    /**
     * 插件状态
     */
    @Schema(description = "插件状态，installed--已安装，notInstall--未安装，installFail--安装失败")
    private String installStatus;

    public void setInstallStatus(String installStatus) {
        this.installStatus = installStatus;
        StackTraceElement[] traceElements = new Exception().getStackTrace();
        StringWriter ap = new StringWriter(128);
        PrintWriter out = new PrintWriter(ap);
        for (int i = 1; i < 6; i++) {
            out.println(traceElements[i]);
        }
        log.warn("更新插件状态:  {} {} {} {}", getName(), getId(), installStatus, ap);
    }

    /**
     * 插件所在路径
     */
    private String plugPath;

    /**
     * 插件安装时间
     */
    private Long installTime;

    /**
     * 插件安装步骤标记
     */
    private Set<String> installStepFlags = new HashSet<>();
    String name;
    String filePath;
    String basePackage;
    String[] beanNames;
    String[] mapperNames;
    String[] controllerNames;
    @Hidden
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonAdapter(ToStringSerializer.class)
    Runnable[] uninstallCallbacks;

    int eventListener;
    long createTime;
    @Hidden
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonAdapter(ToStringSerializer.class)
    URLClassLoader classLoader;

    public int getId() {
        return System.identityHashCode(this);
    }

    public String getName() {
        return plugInfoYml != null ? plugInfoYml.getName() : name;
    }

    public PlugInfo() {
    }

    public void setBaseInfo(String filePath, URLClassLoader classLoader,
                            String basePackage, String[] beanNames, String[] mapperNames) {
        this.filePath = filePath;
        this.classLoader = classLoader;
        this.basePackage = basePackage;
        this.beanNames = beanNames;
        this.mapperNames = mapperNames;
        createTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}