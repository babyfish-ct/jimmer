package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.spring.cloud.SpringCloudExchange;
import org.babyfish.jimmer.sql.runtime.MicroServiceExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
@Conditional(MicroServiceCondition.class)
@ConditionalOnMissingBean(MicroServiceExchange.class)
public class MicroServiceExchangeConfig {

    @Bean
    public MicroServiceExchange microServiceExchange(
            RestTemplate restTemplate,
            JsonCodec<?> jsonCodec
    ) {
        return new SpringCloudExchange(restTemplate, jsonCodec);
    }
}
