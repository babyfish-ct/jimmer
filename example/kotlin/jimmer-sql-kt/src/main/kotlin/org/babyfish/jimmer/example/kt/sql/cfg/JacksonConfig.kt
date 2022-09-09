package org.babyfish.jimmer.example.kt.sql.cfg

import org.babyfish.jimmer.jackson.ImmutableModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    // This module is important,
    // it tells spring how to serialize/deserialize jimmer objects.
    @Bean
    fun immutableModule(): ImmutableModule =
        ImmutableModule()
}