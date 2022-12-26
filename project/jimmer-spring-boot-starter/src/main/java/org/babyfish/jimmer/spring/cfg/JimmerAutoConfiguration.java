package org.babyfish.jimmer.spring.cfg;

import kotlin.Unit;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.spring.client.TypeScriptService;
import org.babyfish.jimmer.spring.repository.SpringConnectionManager;
import org.babyfish.jimmer.spring.repository.config.JimmerRepositoriesRegistrar;
import org.babyfish.jimmer.spring.repository.config.JimmerRepositoryConfigExtension;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClientKt;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.util.List;

@AutoConfiguration(after = { DataSourceAutoConfiguration.class })
@ConditionalOnProperty(
        prefix = "spring.data.jimmer.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(JimmerProperties.class)
@Import(SqlClientConfig.class)
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

