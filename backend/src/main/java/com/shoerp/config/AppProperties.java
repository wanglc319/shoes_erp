package com.shoerp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private boolean initDemoData = true;

    private String defaultPassword = "123456";

    private String uploadDir = "uploads";
}
