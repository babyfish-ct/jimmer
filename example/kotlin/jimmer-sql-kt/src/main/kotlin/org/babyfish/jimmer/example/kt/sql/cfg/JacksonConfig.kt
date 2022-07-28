package org.babyfish.jimmer.example.kt.sql.cfg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.babyfish.jimmer.jackson.ImmutableModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun prettyWriter(): ObjectWriter {
        return ObjectMapper()
            .registerModule(ImmutableModule())
            .writerWithDefaultPrettyPrinter()
    }
}