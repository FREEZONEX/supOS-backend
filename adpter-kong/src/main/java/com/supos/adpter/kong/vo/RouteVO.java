package com.supos.adpter.kong.vo;

import com.supos.common.dto.protocol.KeyValuePair;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RouteVO implements Serializable {

    private static final long serialVersionUID = 1l;

    // name可能是国际化key
    private String name;

    private String showName;

    private MenuVO menu;

    private List<KeyValuePair<String>> tags;

    private ServiceResponseVO service;

    @Data
    @AllArgsConstructor
    public static class MenuVO implements Serializable {

        private static final long serialVersionUID = 1l;

        private String url;

        /**
         * 是否选中
         */
        private boolean picked;

        public MenuVO(String url) {
            this.url = url;
        }
    }



}
