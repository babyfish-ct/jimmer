package org.babyfish.jimmer.spring.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.spring.client.JavaFeignController;
import org.babyfish.jimmer.spring.client.OpenApiController;
import org.babyfish.jimmer.spring.client.OpenApiUiController;
import org.babyfish.jimmer.spring.client.TypeScriptController;
import org.babyfish.jimmer.spring.cloud.MicroServiceExporterController;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

@Conditional(HttpServletCondition.class)
public class ServletControllerConfiguration {

    @ConditionalOnProperty("jimmer.client.ts.path")
    @ConditionalOnMissingBean(TypeScriptController.class)
    @Bean
    public TypeScriptController typeScriptController(JimmerProperties properties) {
        return new TypeScriptController(properties);
    }

    @ConditionalOnProperty("jimmer.client.openapi.path")
    @ConditionalOnMissingBean(OpenApiController.class)
    @Bean
    public OpenApiController openApiController(JimmerProperties properties) {
        return new OpenApiController(properties);
    }

    @ConditionalOnProperty("jimmer.client.openapi.ui-path")
    @ConditionalOnMissingBean(OpenApiUiController.class)
    @Bean
    public OpenApiUiController openApiUiController(JimmerProperties properties) {
        return new OpenApiUiController(properties);
    }

    @ConditionalOnProperty("jimmer.client.java-feign.path")
    @ConditionalOnMissingBean(JavaFeignController.class)
    @Bean
    public JavaFeignController javaFeignController(JimmerProperties properties) {
        return new JavaFeignController(properties);
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
