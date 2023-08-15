package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.spring.client.CodeBasedExceptionAdvice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

@Configuration
@ConditionalOnProperty(
        name = "jimmer.error-translator.disabled",
        havingValue = "false",
        matchIfMissing = true
)
@ConditionalOnClass(ResponseEntity.class)
@ConditionalOnMissingBean(CodeBasedExceptionAdvice.class)
public class ErrorTranslatorConfig {

    @Bean
    public CodeBasedExceptionAdvice codeBasedExceptionAdvice(
            JimmerProperties properties
    ) {
        return new CodeBasedExceptionAdvice(properties);
    }
}
