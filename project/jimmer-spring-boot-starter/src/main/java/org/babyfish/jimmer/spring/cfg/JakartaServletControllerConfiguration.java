package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.spring.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.ParameterNameDiscoverer;

import jakarta.servlet.Servlet;

@ConditionalOnClass(Servlet.class)
public class JakartaServletControllerConfiguration {
    @ConditionalOnProperty("jimmer.client.ts.path")
    @ConditionalOnMissingBean(JakartaTypeScriptController.class)
    @Bean
    public JakartaTypeScriptController typeScriptController(Metadata metadata, JimmerProperties properties) {
        return new JakartaTypeScriptController(metadata, properties);
    }

    @ConditionalOnProperty("jimmer.client.java-feign.path")
    @ConditionalOnMissingBean(JakartaJavaFeignController.class)
    @Bean
    public JakartaJavaFeignController javaFeignController(Metadata metadata, JimmerProperties properties) {
        return new JakartaJavaFeignController(metadata, properties);
    }

    @Conditional(MetadataCondition.class)
    @ConditionalOnMissingBean(Metadata.class)
    @Bean
    public JakartaMetadataFactoryBean metadataFactoryBean(
            ApplicationContext ctx,
            @Autowired(required = false) ParameterNameDiscoverer parameterNameDiscoverer
    ) {
        return new JakartaMetadataFactoryBean(ctx, parameterNameDiscoverer);
    }
}
