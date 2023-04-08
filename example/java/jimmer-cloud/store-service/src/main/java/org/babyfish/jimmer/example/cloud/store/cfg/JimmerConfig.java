package org.babyfish.jimmer.example.cloud.store.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.example.cloud.model.JimmerModule;
import org.babyfish.jimmer.spring.cloud.SpringCloudExchange;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.runtime.MicroServiceExchange;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class JimmerConfig {

    @Bean
    public EntityManager entityManager() {
        return JimmerModule.ENTITY_MANAGER;
    }

    // Jimmer uses this load balanced client to fetch data across micro services
    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
