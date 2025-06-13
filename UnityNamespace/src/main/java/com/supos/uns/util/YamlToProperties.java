package com.supos.uns.util;

import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class YamlToProperties {
    
    public static Properties convert(InputStream yamlInput) {
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(yamlInput);
        Properties properties = new Properties();
        flattenMap("", yamlMap, properties);
        return properties;
    }

    private static void flattenMap(String prefix, Map<String, Object> source, Properties target) {
        source.forEach((key, value) -> {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenMap(fullKey, nestedMap, target);
            } else if (value instanceof List) {
                target.put(fullKey, ((List<?>) value).stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
            } else {
                target.put(fullKey, value.toString());
            }
        });
    }

}
