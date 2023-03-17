package org.babyfish.jimmer.example.kt.graphql.cfg

import graphql.scalars.ExtendedScalars
import graphql.schema.*
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import java.time.LocalDateTime


@Configuration
class GraphQLConfig {

    @Bean
    fun runtimeWiringConfigurer(entityManager: EntityManager): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer {
            it
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(GRAPHQL_LOCAL_DATE_TIME)
        }

    companion object {

        private val GRAPHQL_LOCAL_DATE_TIME = GraphQLScalarType.newScalar()
            .name("LocalDateTime").description("java.time.LocalDateTime")
            .coercing(
                object : Coercing<LocalDateTime, LocalDateTime> {
                    @Throws(CoercingSerializeException::class)
                    override fun serialize(dataFetcherResult: Any): LocalDateTime? {
                        return dataFetcherResult as LocalDateTime
                    }

                    @Throws(CoercingParseValueException::class)
                    override fun parseValue(input: Any): LocalDateTime {
                        throw UnsupportedOperationException()
                    }

                    @Throws(CoercingParseLiteralException::class)
                    override fun parseLiteral(input: Any): LocalDateTime {
                        throw UnsupportedOperationException()
                    }
                }
            )
            .build()
    }
}