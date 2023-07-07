package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.client.CodeBasedExceptionAdvice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "jimmer.error-translator.disabled",
        havingValue = "false",
        matchIfMissing = true
)
@ConditionalOnMissingBean(CodeBasedExceptionAdvice.class)
public class ErrorTranslatorConfig {

    @Bean
    public CodeBasedExceptionAdvice codeBasedExceptionAdvice(
            JimmerProperties properties
    ) {
        return new CodeBasedExceptionAdvice(properties);
    }
}
