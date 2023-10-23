package org.babyfish.jimmer.sql.example.cfg

import org.babyfish.jimmer.spring.SpringJSqlClient
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.toKSqlClient
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
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

    @Bean
    fun sqlClient(ctx: ApplicationContext?, publisher: ApplicationEventPublisher?): KSqlClient {
        return object: SpringJSqlClient(ctx, publisher, true) {

        }.toKSqlClient()
    }
}