package org.babyfish.jimmer.sql.example.graphql.cfg;

import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;

@Configuration
public class GraphQLConfig {

    private static final GraphQLScalarType GRAPHQL_LOCAL_DATE_TIME =
            GraphQLScalarType.newScalar()
                    .name("LocalDateTime").description("java.time.LocalDateTime")
                    .coercing(
                            new Coercing<LocalDateTime, LocalDateTime>() {
                                @Override
                                public LocalDateTime serialize(@NotNull Object dataFetcherResult) throws CoercingSerializeException {
                                    return (LocalDateTime) dataFetcherResult;
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

        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(GRAPHQL_LOCAL_DATE_TIME);
    }
}
