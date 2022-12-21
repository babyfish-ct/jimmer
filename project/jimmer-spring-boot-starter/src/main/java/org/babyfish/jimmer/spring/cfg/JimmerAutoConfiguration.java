package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.spring.client.TypeScriptService;
import org.babyfish.jimmer.spring.repository.config.JimmerRepositoriesRegistrar;
import org.babyfish.jimmer.spring.repository.config.JimmerRepositoryConfigExtension;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = { DataSourceAutoConfiguration.class })
@ConditionalOnProperty(
        prefix = "spring.data.jimmer.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(JimmerProperties.class)
public class JimmerAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(JimmerRepositoryConfigExtension.class)
    @Import(JimmerRepositoriesRegistrar.class)
    static class JimmerRepositoriesConfiguration {

    }

    @ConditionalOnMissingBean(ImmutableModule.class)
    @Bean
    public ImmutableModule immutableModule() {
        return new ImmutableModule();
    }

    @ConditionalOnProperty("jimmer.ts.path")
    @ConditionalOnMissingBean(TypeScriptService.class)
    @Bean
    public TypeScriptService typeScriptService(ApplicationContext ctx) {
        return new TypeScriptService(ctx);
    }
}

