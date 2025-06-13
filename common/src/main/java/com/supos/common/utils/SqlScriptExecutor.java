package com.supos.common.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;

public class SqlScriptExecutor {
    
    /**
     * 执行类路径下的SQL脚本
     * @param scriptPath 脚本路径，如 "sql/init.sql"
     */
    public void executeSqlScript(DataSource dataSource, String scriptPath) throws MalformedURLException {
        File  scriptFile = new File(scriptPath);
        if (!scriptFile.exists() || !scriptFile.isFile()) {
            throw new IllegalArgumentException("脚本文件未找到: " + scriptPath);
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new UrlResource(scriptFile.toURI()));
        populator.setSeparator(ScriptUtils.DEFAULT_STATEMENT_SEPARATOR); // 默认是;
        populator.setContinueOnError(false); // 遇到错误是否继续
        
        try (Connection connection = dataSource.getConnection()) {
            populator.populate(connection);
        } catch (Exception e) {
            throw new RuntimeException("执行SQL脚本失败: " + scriptPath, e);
        }
    }

    /**
     * 执行类路径下的SQL脚本
     * @param scriptFile 脚本路径，如 "sql/init.sql"
     */
    public void executeSqlScript(DataSource dataSource, File scriptFile) throws MalformedURLException {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new UrlResource(scriptFile.toURI()));
        populator.setSeparator(ScriptUtils.DEFAULT_STATEMENT_SEPARATOR); // 默认是;
        populator.setContinueOnError(false); // 遇到错误是否继续

        try (Connection connection = dataSource.getConnection()) {
            populator.populate(connection);
        } catch (Exception e) {
            throw new RuntimeException("执行SQL脚本失败: " + scriptFile, e);
        }
    }
    
    /**
     * 执行多个SQL脚本
     * @param scriptPaths 脚本路径数组
     */
    public void executeSqlScripts(DataSource dataSource, ClassLoader classLoader, String... scriptPaths) {
        if (scriptPaths == null || scriptPaths.length == 0) {
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        for (String path : scriptPaths) {
            URL scriptUrl = classLoader.getResource(path);
            if (scriptUrl == null) {
                throw new IllegalArgumentException("脚本文件未找到: " + path);
            }
            populator.addScript(new ClassPathResource(path));
        }
        
        try (Connection connection = dataSource.getConnection()) {
            populator.populate(connection);
        } catch (Exception e) {
            throw new RuntimeException("执行SQL脚本失败", e);
        }
    }
}