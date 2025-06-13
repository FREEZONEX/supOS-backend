package com.supos.common.utils;

import com.supos.common.exception.BuzException;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
public class MyYamlUtils {

    public static <T> T loadYaml(String path, Class<T> clazz) {
        try (InputStream input = new FileInputStream(path)) {
            return loadYaml(input, clazz);
        } catch (Exception e) {
            log.error("yaml文件解析异常", e);
            throw new BuzException("文件解析异常：path=" + path);
        }
    }

    public static <T> T loadYaml(InputStream input, Class<T> clazz) {
        // 自定义构造器配置
        Constructor constructor = new Constructor(clazz);
        constructor.setPropertyUtils(new PropertyUtils() {
            @Override
            public Property getProperty(Class<?> type, String name) {
                setSkipMissingProperties(true);  // 核心：忽略未知字段
                return super.getProperty(type, name);
            }
        });

        // 加载并解析 YAML
        Yaml yaml = new Yaml(constructor);
        return yaml.loadAs(input, clazz);

    }
}
