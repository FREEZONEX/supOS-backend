package com.supos.uns.config;

import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.supos.common.utils.DateTimeUtils;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DatabasePopulator.class)
@Conditional(DbScriptInitConfig.OnBatchDatasourceInitializationCondition.class)
@EnableConfigurationProperties(BatchProperties.class)
public class DbScriptInitConfig implements BeanPostProcessor {

    @Bean
    @ConditionalOnMissingBean({BatchDataSourceScriptDatabaseInitializer.class})
    BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(DataSource dataSource,
                                                                        @BatchDataSource ObjectProvider<DataSource> batchDataSource, BatchProperties properties) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource ds = (HikariDataSource) dataSource;
            tryCreateSchema(ds, ds.getJdbcUrl());
        }
        return new BatchDataSourceScriptDatabaseInitializer(batchDataSource.getIfAvailable(() -> dataSource),
                properties.getJdbc());
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DefaultDataSourceCreator) {
            return new InitializerDefaultDataSourceCreator((DefaultDataSourceCreator) bean);
        }
        return bean;
    }

    static class InitializerDefaultDataSourceCreator extends DefaultDataSourceCreator {
        final DefaultDataSourceCreator tar;

        InitializerDefaultDataSourceCreator(DefaultDataSourceCreator tar) {
            this.tar = tar;
        }

        public DataSource createDataSource(DataSourceProperty dataSourceProperty) {
            DataSource ds = tar.createDataSource(dataSourceProperty);
            tryCreateSchema(ds, dataSourceProperty.getUrl());
            return ds;
        }

    }

    private static void tryCreateSchema(DataSource ds, String url) {
        if (url.indexOf('?') > 0 && url.startsWith("jdbc:")) {
            int protoIndex = url.indexOf("://");
            String type = url.substring(5, protoIndex);
            if ("postgresql".equals(type)) {
                String uri = url.substring(protoIndex);
                UriComponents components = UriComponentsBuilder.fromUriString("http" + uri).build();
                MultiValueMap<String, String> queryMap = components.getQueryParams();
                String currentSchema = queryMap.getFirst("currentSchema");
                if (StringUtils.hasText(currentSchema)) {
                    try (Connection conn = ds.getConnection()) {
                        try (Statement statement = conn.createStatement()) {
                            String checkSql = "select data_type FROM information_schema.columns where " +
                                    " table_schema = '" + currentSchema + "' and table_name='uns_namespace' and column_name='id'";
                            try (ResultSet resultSet = statement.executeQuery(checkSql)) {
                                if (resultSet.next()) {
                                    String dataType = resultSet.getString(1);
                                    if (!"bigint".equalsIgnoreCase(dataType)) {
                                        String backupSchema = "zzBackup_" + currentSchema + "_" + DateTimeUtils.dateSimple();
                                        log.warn("数据类型定义不兼容， 重建 schema: {}， 备份为 {}", currentSchema, backupSchema);
                                        statement.execute("ALTER SCHEMA " + currentSchema +
                                                " RENAME TO " + backupSchema);
                                    }
                                }
                            }
                            String ddl = "create schema if not exists " + currentSchema;
                            log.info("postgresql initSchema: {}, jdbcUrl = {}", currentSchema, url);
                            statement.execute(ddl);
                        }
                    } catch (Exception ex) {
                        log.error("Fail to init schema: " + currentSchema, ex);
                    }
                }
            }
        }
    }

    static class OnBatchDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

        OnBatchDatasourceInitializationCondition() {
            super("Batch", "spring.batch.jdbc.initialize-schema", "spring.batch.initialize-schema");
        }

    }
}
