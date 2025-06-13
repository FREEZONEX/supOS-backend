package com.supos.adpter.pg.config;

import com.supos.adpter.pg.PostgresqlEventHandler;
import com.supos.adpter.pg.TimeScaleDbEventHandler;
import com.supos.common.adpater.DataSourceProperties;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import com.supos.common.service.IUnsDefinitionService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.sql.SQLException;

@Slf4j
@Configuration("pgConfig")
public class PostgresqlConfig {

    @Bean
    @Order(1)
    public PostgresqlEventHandler pgHandler(@Value("${spring.datasource.url}") String suposJdbcUrl,
                                            @Value("${spring.datasource.username:postgres}") String user,
                                            @Value("${spring.datasource.password:postgres}") String password,
                                            @Value("${pg.schema:public}") String schema) throws SQLException {
        JdbcTemplate template = jdbcTemplate(null, suposJdbcUrl, user, password, schema);
        return new PostgresqlEventHandler(template);
    }

    @Bean
    @ConditionalOnMissingBean(TimeSequenceDataStorageAdapter.class)
    @ConditionalOnProperty("pg.jdbcUrl")
    @Order
    public TimeScaleDbEventHandler timeScaleDbEventHandler(
            @Value("${pg.jdbcUrl}") String tsdbUrl,
            @Value("${tsdb.username:postgres}") String user,
            @Value("${tsdb.password:postgres}") String password,
            @Value("${tsdb.schema:public}") String schema,
            @Autowired IUnsDefinitionService unsDefinitionService
    ) throws SQLException {
        JdbcTemplate template = jdbcTemplate(tsdbUrl, null, user, password, schema);
        return new TimeScaleDbEventHandler(template, unsDefinitionService);
    }


    static class PgDataSource extends HikariDataSource implements DataSourceProperties {
        PgDataSource(HikariConfig config) {
            super(config);
        }

        @Override
        public String getUrl() {
            return super.getJdbcUrl();
        }
    }

    private @Value("${PG_MIN_IDLE:5}") int minIdle;
    private @Value("${PG_MAX_POOL_SIZE:20}") int maxPoolSize;
    private @Value("${PG_CONNECTION_TIMEOUT:9000}") int connectionTimeout;
    private @Value("${PG_MAX_LITE_TIME:15000}") int maxLiteTime;
    private @Value("${PG_IDLE_TIMEOUT:10000}") int idleTimeout;
    private @Value("${PG_LEAK_DETECT_TIMEOUT:60000}") int leakDetectionThreshold;


    private JdbcTemplate jdbcTemplate(String jdbcUrl,
                                      String suposJdbcUrl,
                                      String user,
                                      String password,
                                      String schema// 写关系表数据的 schema
    ) {
        if (!StringUtils.hasText(jdbcUrl)) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                jdbcUrl = "jdbc:postgresql://100.100.100.20:31014/postgres";
            } else if (suposJdbcUrl.startsWith("jdbc:postgresql:")) {
                jdbcUrl = suposJdbcUrl;
            } else {
                jdbcUrl = "jdbc:postgresql://postgresql:5432/postgres";
            }
        }
        HikariConfig config = new HikariConfig();
        config.setUsername(user);
        config.setPassword(password);
        config.setSchema(schema);
        // connection pool configurations
        config.setMinimumIdle(minIdle); // minimum number of idle connection
        config.setMaximumPoolSize(maxPoolSize); // maximum number of connection in the pool
        config.setConnectionTimeout(connectionTimeout); // maximum wait milliseconds for get connection from pool
        config.setMaxLifetime(maxLiteTime); // maximum life time for each connection
        config.setIdleTimeout(idleTimeout); // max idle time for recycle idle connection]
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        config.setConnectionTestQuery("SELECT 1"); // validation query
        config.setJdbcUrl(jdbcUrl);
        config.setAutoCommit(true);
        HikariDataSource dataSource = new PgDataSource(config); // create datasource
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.execute("create schema if not exists " + schema);
        return template;
    }



}
