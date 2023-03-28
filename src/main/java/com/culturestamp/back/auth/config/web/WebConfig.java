package com.culturestamp.back.auth.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("http://localhost:3000","http://localhost:8080","https://localhost:3000",","https://localhost:8080",
                "https://ec2-3-35-144-181.ap-northeast-2.compute.amazonaws.com", "https://ec2-3-35-144-181.ap-northeast-2.compute.amazonaws.com:8080")
            .allowedMethods("GET", "POST", "PUT","DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3000);
    }
}
