package com.supos.adpter.kong.vo;

import lombok.Data;

import java.util.List;

@Data
public class ServiceResponseVO {

    private String id;

    private String name;

    private String host;

    private String path;

    private int port;

    private String protocol;

    private List<String> tags;
}