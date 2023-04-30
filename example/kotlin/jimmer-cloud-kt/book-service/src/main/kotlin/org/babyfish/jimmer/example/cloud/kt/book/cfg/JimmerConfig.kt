package org.babyfish.jimmer.example.cloud.kt.book.cfg

import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class JimmerConfig {

    // Jimmer uses this load balanced client to fetch data across microservices
    @LoadBalanced
    @Bean
    fun restTemplate() = RestTemplate()
}