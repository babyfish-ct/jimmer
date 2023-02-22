package org.babyfish.jimmer.spring.repository.config;

import org.babyfish.jimmer.spring.repository.support.JimmerRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(
        prefix = "spring.data.jimmer.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnMissingBean({ JimmerRepositoryFactoryBean.class, JimmerRepositoryConfigExtension.class })
public class JimmerRepositoriesConfig {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(JimmerRepositoryConfigExtension.class)
    @Import(JimmerRepositoriesRegistrar.class)
    static class JimmerRepositoriesConfiguration {}
}
