package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.spring.client.JavaFeignController;
import org.babyfish.jimmer.spring.client.MetadataFactoryBean;
import org.babyfish.jimmer.spring.client.TypeScriptController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.ParameterNameDiscoverer;

import javax.servlet.Servlet;

@ConditionalOnClass(Servlet.class)
public class ServletControllerConfiguration {
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
}
