package org.babyfish.jimmer.example.cloud.kt.store.cfg

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class JimmerConfig {

    // Jimmer uses this load balanced client to fetch data across microservices
    @LoadBalanced
    @Bean
    fun restTemplate(): RestTemplate =
        RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(1))
            .setReadTimeout(Duration.ofSeconds(2))
            .build()
}