package com.supos.adpter.tdengine.config;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.supos.adpter.tdengine.TdEngineEventHandler;
import com.supos.common.adpater.DataSourceProperties;
import com.supos.common.service.IUnsDefinitionService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Slf4j
@Configuration
public class TdEngineConfig {

    @Bean("tdEngineEventHandler")
    @Order(2)
    public TdEngineEventHandler tdEngineEventHandler(@Value("${td.jdbcUrl:}") String jdbcUrl,
                                                     @Value("${TD_ENGINE_ROOT_USER:root}") String user,
                                                     @Value("${TDENGINE_ROOT_PASSWORD:taosdata}") String password,
                                                     @Value("${TDENGINE_DB_NAME:public}") String dbName,
                                                     @Autowired IUnsDefinitionService unsDefinitionService
    ) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(jdbcUrl, user, password, dbName);
        if (jdbcTemplate == null) {
            return null;
        }
        return new TdEngineEventHandler(jdbcTemplate, unsDefinitionService);
    }

    private JdbcTemplate getJdbcTemplate(String jdbcUrl,
                                         String user,
                                         String password,
                                         String dbName
    ) {
        if (!StringUtils.hasText(jdbcUrl)) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                jdbcUrl = "jdbc:TAOS-WS://100.100.100.20:31016/"; //ws: 6041, rest: 6030
            } else {
                jdbcUrl = "jdbc:TAOS-WS://tdengine:6041/";
            }
        }
        log.info("TdEngine jdbcUrl: {}", jdbcUrl);
        try (HttpResponse response = HttpUtil.createPost("http" + jdbcUrl.substring(jdbcUrl.indexOf("://")) + "rest/sql")
                .header("Authorization", "Basic cm9vdDp0YW9zZGF0YQ==").body("show databases").execute()) {
            log.info("测试 TdEngine http Code={}, body={}", response.getStatus(), response.body());
        } catch (Exception ex) {
            log.error("连不上TDEngine! {}", ex.getMessage());
            return null;
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setCatalog(dbName);
        config.setSchema(dbName);
        // connection pool configurations
        config.setMinimumIdle(10); // minimum number of idle connection
        config.setMaximumPoolSize(10); // maximum number of connection in the pool
        config.setConnectionTimeout(30000); // maximum wait milliseconds for get connection from pool
        config.setMaxLifetime(0); // maximum life time for each connectionT
        config.setIdleTimeout(0); // max idle time for recycle idle connection
        config.setConnectionTestQuery("SELECT SERVER_VERSION()"); // validation query
        HikariDataSource dataSource = new TdDs(config); // create datasource
        JdbcTemplate template = new JdbcTemplate(dataSource);
        try {
            template.execute("create database if not exists `" + dbName + "` ");
        } catch (Exception ex) {
            log.error("TDEngine 建库出错! {}", ex.getMessage());
            return null;
        }
        template = getDirectJdbcTemplate(jdbcUrl, template, config);
        return template;
    }

    private JdbcTemplate getDirectJdbcTemplate(String jdbcUrl, JdbcTemplate template, HikariConfig config) {
        if (jdbcUrl.startsWith("jdbc:TAOS://")) {
            return template;
        }
        String version = template.query("SELECT SERVER_VERSION()", rs -> rs.next() ? rs.getString(1) : null);
        String os = System.getProperty("os.name").toLowerCase();
        log.info("TdEngineServer 版本 = {}, os: {}", version, os);
        if ("3.3.5.0".equals(version) && (!os.contains("windows") || os.contains("linux"))) {
            boolean hasSo = false;
            String libPath = System.getProperty("java.library.path");
            String lastPath = libPath.substring(libPath.lastIndexOf(File.pathSeparatorChar) + 1);
            log.info("lastPath = {}, libPath = {}", lastPath, libPath);
            try {
                File soFile = new File(lastPath, "libtaos.so");
                if (!soFile.exists()) {
                    soFile.getParentFile().mkdirs();
                    InputStream so = getClass().getClassLoader().getResourceAsStream("libs/libtaos.so.3.3.5.0");
                    if (so != null) {
                        try (FileOutputStream fo = new FileOutputStream(soFile)) {
                            StreamUtils.copy(so, fo);
                            hasSo = true;
                        } finally {
                            so.close();
                        }
                    }
                } else {
                    hasSo = true;
                }
            } catch (Exception ex) {
            }
            if (hasSo) {
                // jdbc:TAOS://taosdemo.com:6030/power?user=root&password=taosdata
                final String directJdbcUrl = "jdbc:TAOS://tdengine:6030/" + config.getSchema()
                        + "?user=" + config.getUsername() + "&password=" + config.getPassword();
                config.setJdbcUrl(directJdbcUrl);
                try {
                    JdbcTemplate templateDirect = new JdbcTemplate(new TdDs(config));
                    log.info("TdEngine 切换为本地连接模式, jdbcUrl=" + directJdbcUrl);
                    template = templateDirect;
                } catch (Throwable ex) {
                    log.warn("TdEngine 本地连接模式 切换失败!", ex);
                    config.setJdbcUrl(jdbcUrl);
                }
            }
        }
        return template;
    }

    static class TdDs extends HikariDataSource implements DataSourceProperties {
        TdDs(HikariConfig config) {
            super(config);
        }

        @Override
        public String getUrl() {
            return super.getJdbcUrl();
        }
    }
}
