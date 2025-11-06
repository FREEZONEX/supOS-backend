package com.supos.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import org.springframework.util.StringUtils;

public enum FileReadWriteMode {
    READ_ONLY("READ_ONLY"),
    READ_WRITE("READ_WRITE");
 
    @Getter
    private final String mode;
 
    @JsonCreator
    public static FileReadWriteMode getByNameIgnoreCase(String name) {
        if (StringUtils.hasText(name)) {
            for (FileReadWriteMode v : FileReadWriteMode.values()) {
                if (v.mode.equalsIgnoreCase(name)) {
                    return v;
                }
            }
        }
        return null;
    }
 
    FileReadWriteMode(String mode) {
        this.mode = mode;
    }
}