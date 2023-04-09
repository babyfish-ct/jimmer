package org.babyfish.jimmer.example.cloud.kt.author.cfg

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenAPIConfig {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI().info(Info().title("cloud:author-service").version("0.7.31"))
}