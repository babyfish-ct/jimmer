package org.babyfish.jimmer.spring.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.spring.cloud.MicroServiceExporterController;
import org.babyfish.jimmer.spring.repository.config.JimmerRepositoriesConfig;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(JimmerProperties.class)
@Import({SqlClientConfig.class, JimmerRepositoriesConfig.class})
public class JimmerAutoConfiguration {

    @ConditionalOnMissingBean(ImmutableModule.class)
    @Bean
    public ImmutableModule immutableModule() {
        return new ImmutableModule();
    }

    @Conditional(MicroServiceCondition.class)
    @ConditionalOnMissingBean(MicroServiceExporterController.class)
    @Bean
    public MicroServiceExporterController microServiceExporterController(
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

