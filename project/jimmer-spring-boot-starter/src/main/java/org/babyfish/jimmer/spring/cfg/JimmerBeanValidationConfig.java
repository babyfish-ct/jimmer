package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.validation.JimmerValidationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.validation.beanvalidation.LocalValidatorFactoryBean")
public class JimmerBeanValidationConfig {

    @Bean
    public static BeanPostProcessor jimmerValidationBeanPostProcessor() {
        return new JimmerValidationBeanPostProcessor();
    }
}
