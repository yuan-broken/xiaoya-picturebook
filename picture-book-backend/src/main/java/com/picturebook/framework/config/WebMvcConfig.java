package com.picturebook.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

/**
 * Web MVC 配置
 * - 本地文件存储路径映射（/uploads/** → 本地目录）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${picturebook.file.local.base-path:./uploads}")
    private String localBasePath;

    @Value("${picturebook.file.local.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/** 映射到本地目录
        String location = new File(localBasePath).getAbsolutePath().replace("\\", "/") + "/";
        if (!location.startsWith("file:")) {
            location = "file:" + location;
        }
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(location);
    }
}
