package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.client.CodeBasedExceptionAdvice;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorWrapperConfig {

    @Bean
    public CodeBasedExceptionAdvice codeBasedExceptionAdvice(
            JimmerProperties properties
    ) {
        return new CodeBasedExceptionAdvice(properties);
    }
}
