package org.babyfish.jimmer.spring.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.spring.cloud.SpringCloudExchange;
import org.babyfish.jimmer.sql.runtime.MicroServiceExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.client.RestTemplate;

@Conditional(MicroServiceCondition.class)
@ConditionalOnMissingBean(MicroServiceExchange.class)
public class MicroServiceExchangeConfig {

    @Bean
    public MicroServiceExchange microServiceExchange(
            RestTemplate restTemplate,
            ObjectMapper mapper
    ) {
        return new SpringCloudExchange(restTemplate, mapper);
    }
}
