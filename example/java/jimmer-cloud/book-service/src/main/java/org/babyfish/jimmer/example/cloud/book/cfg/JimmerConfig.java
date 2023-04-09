package org.babyfish.jimmer.example.cloud.book.cfg;

import org.babyfish.jimmer.example.cloud.model.JimmerModule;
import org.babyfish.jimmer.sql.runtime.EntityManager;
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

    // 待解决问题
    // 什么鬼？在当前项目声明就可以被负载均衡器调，在starter中声明就不能被负载均衡器调（直接访问仍然可以）
//    @Bean
//    public MicroServiceExporterAgent microServiceExporterAgent(JSqlClient sqlClient, ObjectMapper mapper) {
//        return new MicroServiceExporterController(sqlClient, mapper);
//    }
}
