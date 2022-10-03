package org.babyfish.jimmer.sql.example.cfg;

import org.babyfish.jimmer.jackson.ImmutableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // This module is important,
    // it tells spring how to serialize/deserialize jimmer objects.
    @Bean
    public ImmutableModule immutableModule() {
        return new ImmutableModule();
    }
}
