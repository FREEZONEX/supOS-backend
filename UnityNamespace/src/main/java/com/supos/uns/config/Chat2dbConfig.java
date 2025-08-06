package com.supos.uns.config;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "chat2db.enable", matchIfMissing = true)
public class Chat2dbConfig {
    @Value("${chat2db.address:}")
    String chat2dbHost;
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @EventListener(classes = ContextRefreshedEvent.class)
    void initDataSource() {
        final String CHAT2DB_USER = "chat2db_query";
        final String CHAT2DB_PSW = "12321";
        final String CHAT2DB_PG_ALIAS = "@postgresql";
        // 创建 postgres 只读用户，并且限制只能查询 schema=public 的表
        String[] createReadOnlyUserSqls = new String[]{
                "CREATE USER " + CHAT2DB_USER + " WITH PASSWORD '" + CHAT2DB_PSW + "'",
                "GRANT CONNECT ON DATABASE postgres TO " + CHAT2DB_USER,
                "GRANT USAGE ON SCHEMA public TO " + CHAT2DB_USER,
                "GRANT SELECT ON ALL TABLES IN SCHEMA public TO " + CHAT2DB_USER,
                "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO " + CHAT2DB_USER,
        };

        String defaultDataSource = "{\n" +
                "    \"ssh\": {\n" +
                "        \"use\": false,\n" +
                "        \"hostName\": \"\",\n" +
                "        \"port\": \"22\",\n" +
                "        \"userName\": \"\",\n" +
                "        \"localPort\": \"\",\n" +
                "        \"authenticationType\": \"password\",\n" +
                "        \"password\": \"\"\n" +
                "    },\n" +
                "    \"driverConfig\": {\n" +
                "        \"jdbcDriverClass\": \"org.postgresql.Driver\",\n" +
                "        \"jdbcDriver\": \"postgresql-42.5.1.jar\"\n" +
                "    },\n" +
                "    \"alias\": \"" + CHAT2DB_PG_ALIAS + "\",\n" +
                "    \"environmentId\": 1,\n" +
                "    \"host\": \"postgresql\",\n" +
                "    \"port\": \"5432\",\n" +
                "    \"authenticationType\": \"1\",\n" +
                "    \"user\": \"" + CHAT2DB_USER + "\",\n" +
                "    \"password\": \"" + CHAT2DB_PSW + "\",\n" +
                "    \"database\": \"postgres\",\n" +
                "    \"url\": \"jdbc:postgresql://postgresql:5432/postgres\",\n" +
                "    \"extendInfo\": [],\n" +
                "    \"connectionEnvType\": \"DAILY\",\n" +
                "    \"type\": \"POSTGRESQL\"\n" +
                "}";
        if (StringUtils.isBlank(chat2dbHost)) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                return;
            } else {
                chat2dbHost = "http://chat2db:10824";
            }
        }
        if (!chat2dbHost.startsWith("http")) {
            chat2dbHost = "http://" + chat2dbHost;
        }
        if (chat2dbHost.endsWith("/")) {
            chat2dbHost = chat2dbHost.substring(0, chat2dbHost.length() - 1);
        }
        final String HOST = chat2dbHost;
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.submit(() -> {
            final AtomicBoolean addUserOk = new AtomicBoolean(addPgReadOnlyUser(createReadOnlyUserSqls));
            executorService.submit(new Runnable() {
                long i = 1;

                @Override
                public void run() {
                    try {
                        if (!addUserOk.get()) {
                            addUserOk.set(addPgReadOnlyUser(createReadOnlyUserSqls));
                        }
                        addDefaultDataSourceToChat2db(HOST, CHAT2DB_PG_ALIAS, defaultDataSource);
                        log.info("chat2db 添加数据源. {}", HOST);
                        executorService.shutdown();
                    } catch (Exception ex) {
                        log.error("chat2db 添加数据源失败{}", HOST);
                        executorService.schedule(this, (i++) << 2, TimeUnit.SECONDS);
                    }
                }
            });
        });
    }

    private boolean addPgReadOnlyUser(String[] createReadOnlyUserSqls) {
        try {
            DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.batchUpdate(createReadOnlyUserSqls);
            log.info("创建 CHAT2DB_USER 成功!");
            return true;
        } catch (Exception ex) {
            Throwable cause = ex, pre = ex;
            while (pre != null) {
                cause = pre;
                pre = pre.getCause();
            }
            String msg = cause.getMessage();
            if (msg != null && msg.toLowerCase().contains("exists")) {
                if (msg.startsWith("ERROR:")) {
                    msg = msg.substring(6);
                }
                log.info("已经创建 CHAT2DB_USER： {}", msg);
                return true;
            } else {
                log.error("创建 CHAT2DB_USER", ex);
            }
        }
        return false;
    }

    private static void addDefaultDataSourceToChat2db(String HOST, String CHAT2DB_PG_ALIAS, String defaultDataSource) {
        HttpUtil.createPost(HOST + "/api/oauth/login_a").timeout(3000)
                .body("{\"userName\":\"chat2db\",\"password\":\"chat2db\"}").then(resp -> {
                    String setCookie = resp.header("Set-Cookie");
                    log.info("CHAT2DB 登录结果[{}]：{}", resp.getStatus(), resp.headers());
                    if (setCookie != null) {
                        HttpUtil.createGet(HOST + "/api/connection/datasource/list?searchKey=" + CHAT2DB_PG_ALIAS +
                                        "&pageSize=5").timeout(5000).header("Cookie", setCookie)
                                .then(queryRs -> {
                                    String body = queryRs.body();
                                    log.debug("CHAT2DB 数据源查询结果：{}", body);
                                    if (body == null || !body.contains("postgresql://")) {
                                        HttpUtil.createPost(HOST + "/api/connection/datasource/create").timeout(5000).header("Cookie", setCookie)
                                                .body(defaultDataSource).then(rs -> {
                                                    log.info("CHAT2DB 创建数据源结果[{}]：{}", rs.getStatus(), rs.body());
                                                });
                                    }
                                });

                    } else {
                        log.warn("CHAT2DB 登录失败!");
                    }
                });
    }
}
