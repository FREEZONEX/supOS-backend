package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum SubscribeTypeEnum {

    FOLDER(0,""),
    TEMPLATE(1,"template/"),
    LABEL(3,"label/"),

    ;

    private Integer type;

    /**
     * topic前缀
     */
    private String topicPrefix;




    public static SubscribeTypeEnum parse(int type) {
        for (SubscribeTypeEnum each : values()) {
            if (type == each.type) {
                return each;
            }
        }
        return null;
    }
}

