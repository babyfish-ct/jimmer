package org.babyfish.jimmer.sql.example.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectWriter prettyWriter() {
        return new ObjectMapper()
                .registerModule(new ImmutableModule())
                .writerWithDefaultPrettyPrinter();
    }
}
