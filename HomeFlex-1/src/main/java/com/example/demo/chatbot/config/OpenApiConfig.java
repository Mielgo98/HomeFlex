package com.example.demo.chatbot.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "HomeFlex Chatbot API",
        version = "1.0",
        description = "API para el chatbot de HomeFlex"
    )
)
public class OpenApiConfig {
}