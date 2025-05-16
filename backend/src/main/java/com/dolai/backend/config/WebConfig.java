package com.dolai.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",
			"http://13.209.37.189:5173",
                        "http://ec2-13-209-37-189.ap-northeast-2.compute.amazonaws.com:5173"
                )
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
