package com.mytool.zktool.entity;

import lombok.Data;

@Data
public class Cluster {
    private Long id;
    private String name;
    private String host;
    private Integer port;
    private Integer retryCount;
    private Integer timeout;
    private String description;
} 