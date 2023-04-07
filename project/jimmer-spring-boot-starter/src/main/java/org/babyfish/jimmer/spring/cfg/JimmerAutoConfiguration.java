package org.babyfish.jimmer.spring.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.spring.client.JavaFeignController;
import org.babyfish.jimmer.spring.client.MetadataFactoryBean;
import org.babyfish.jimmer.spring.client.TypeScriptController;
import org.babyfish.jimmer.spring.cloud.MicroServiceExporterAgent;
import org.babyfish.jimmer.spring.cloud.MicroServiceExporterController;
import org.babyfish.jimmer.spring.repository.config.JimmerRepositoriesConfig;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterNameDiscoverer;

@AutoConfiguration(after = { DataSourceAutoConfiguration.class })
@EnableConfigurationProperties(JimmerProperties.class)
@Import({SqlClientConfig.class, JimmerRepositoriesConfig.class})
public class JimmerAutoConfiguration {

    @ConditionalOnMissingBean(ImmutableModule.class)
    @Bean
    public ImmutableModule immutableModule() {
        return new ImmutableModule();
    }

    @ConditionalOnProperty("jimmer.client.ts.path")
    @ConditionalOnMissingBean(TypeScriptController.class)
    @Bean
    public TypeScriptController typeScriptController(Metadata metadata, JimmerProperties properties) {
        return new TypeScriptController(metadata, properties);
    }

    @ConditionalOnProperty("jimmer.client.java-feign.path")
    @ConditionalOnMissingBean(JavaFeignController.class)
    @Bean
    public JavaFeignController javaFeignController(Metadata metadata, JimmerProperties properties) {
        return new JavaFeignController(metadata, properties);
    }

    @Conditional(MetadataCondition.class)
    @ConditionalOnMissingBean(Metadata.class)
    @Bean
    public MetadataFactoryBean metadataFactoryBean(
            ApplicationContext ctx,
            @Autowired(required = false) ParameterNameDiscoverer parameterNameDiscoverer
    ) {
        return new MetadataFactoryBean(ctx, parameterNameDiscoverer);
    }

    @Conditional(MicroServiceCondition.class)
    @ConditionalOnMissingBean(MicroServiceExporterAgent.class)
    @Bean
    public MicroServiceExporterAgent microServiceExporterAgent(
            @Autowired(required = false) JSqlClient jSqlClient,
            @Autowired(required = false) KSqlClient kSqlClient,
            ObjectMapper objectMapper
    ) {
        return new MicroServiceExporterController(
                jSqlClient != null ? jSqlClient : kSqlClient.getJavaClient(),
                objectMapper
        );
    }
}

