package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.jackson.ImmutableModule;
import org.springdoc.core.OpenAPIService;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import static org.springdoc.core.Constants.SPRINGDOC_ENABLED;

@Configuration
@ConditionalOnClass(OpenAPIService.class)
@ConditionalOnProperty(name = SPRINGDOC_ENABLED, matchIfMissing = true)
public class SpringDocConfig {

    @Bean
    @ConditionalOnMissingBean
    @Lazy(false)
    ObjectMapperProvider springDocObjectMapperProvider(SpringDocConfigProperties springDocConfigProperties) {
        ObjectMapperProvider provider = new ObjectMapperProvider(springDocConfigProperties);
        provider.jsonMapper().registerModule(new ImmutableModule());
        return provider;
    }
}
