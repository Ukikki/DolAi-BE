package com.dolai.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String currentDir = System.getProperty("user.dir");
        String uploadPath;

        if (currentDir.endsWith("backend")) {
            uploadPath = currentDir + "/uploads/";
        } else {
            uploadPath = currentDir + "/backend/uploads/";
        }

        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + uploadPath);
    }
}