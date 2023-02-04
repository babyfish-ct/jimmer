package org.babyfish.jimmer.example.kt.sql.cfg

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedHeaders("*")
            .allowedMethods("*")
            .maxAge(1800)
            .allowedOrigins("*")
    }
}