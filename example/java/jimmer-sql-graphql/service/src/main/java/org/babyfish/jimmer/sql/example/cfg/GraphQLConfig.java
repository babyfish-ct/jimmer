package org.babyfish.jimmer.sql.example.cfg;

import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class GraphQLConfig {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final GraphQLScalarType GRAPHQL_LOCAL_DATE_TIME =
            GraphQLScalarType.newScalar()
                    .name("LocalDateTime").description("java.time.LocalDateTime")
                    .coercing(
                            new Coercing<LocalDateTime, String>() {
                                @Override
                                public String serialize(@NotNull Object dataFetcherResult) throws CoercingSerializeException {
                                    return DATE_TIME_FORMATTER.format((LocalDateTime)dataFetcherResult);
                                }

                                @Override
                                public @NotNull LocalDateTime parseValue(@NotNull Object input) throws CoercingParseValueException {
                                    throw new UnsupportedOperationException();
                                }

                                @Override
                                public @NotNull LocalDateTime parseLiteral(@NotNull Object input) throws CoercingParseLiteralException {
                                    throw new UnsupportedOperationException();
                                }
                            }
                    )
                    .build();

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> {
            wiringBuilder
                    .scalar(ExtendedScalars.GraphQLLong)
                    .scalar(ExtendedScalars.GraphQLBigDecimal)
                    .scalar(GRAPHQL_LOCAL_DATE_TIME);
        };
    }
}
