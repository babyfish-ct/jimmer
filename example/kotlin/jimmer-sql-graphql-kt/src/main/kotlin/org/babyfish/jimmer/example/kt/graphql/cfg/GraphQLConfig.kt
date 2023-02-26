package org.babyfish.jimmer.example.kt.graphql.cfg

import graphql.scalars.ExtendedScalars
import graphql.schema.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.TargetLevel
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.jetbrains.annotations.NotNull
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import java.time.LocalDateTime


@Configuration
class GraphQLConfig {

    private val GRAPHQL_LOCAL_DATE_TIME = GraphQLScalarType.newScalar()
        .name("LocalDateTime").description("java.time.LocalDateTime")
        .coercing(
            object : Coercing<LocalDateTime, LocalDateTime> {
                @Throws(CoercingSerializeException::class)
                override fun serialize(dataFetcherResult: Any): LocalDateTime? {
                    return dataFetcherResult as LocalDateTime
                }

                @NotNull
                @Throws(CoercingParseValueException::class)
                override fun parseValue(input: Any): LocalDateTime {
                    throw UnsupportedOperationException()
                }

                @NotNull
                @Throws(CoercingParseLiteralException::class)
                override fun parseLiteral(@NotNull input: Any): LocalDateTime {
                    throw UnsupportedOperationException()
                }
            }
        )
        .build()

    @Bean
    fun runtimeWiringConfigurer(entityManager: EntityManager): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer {
            it
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(GRAPHQL_LOCAL_DATE_TIME)
            registerJimmerDataFetchers(it, entityManager)
        }

    companion object {

        /**
         * In next version, this behavior will be embedded into jimmer
         */
        private fun registerJimmerDataFetchers(
            wiringBuilder: RuntimeWiring.Builder,
            entityManager: EntityManager
        ) {
            for (type in entityManager.allTypes) {
                if (type.isEntity) {
                    val typeBuilder = TypeRuntimeWiring
                        .newTypeWiring(type.javaClass.simpleName)
                    for (prop in type.props.values) {
                        if (prop.isAssociation(TargetLevel.ENTITY) || prop.hasTransientResolver()) {
                            typeBuilder.dataFetcher(prop.name, JimmerComplexFetcher(prop))
                        } else {
                            typeBuilder.dataFetcher(prop.name, JimmerSimpleFetcher(prop.id))
                        }
                    }
                    wiringBuilder.type(typeBuilder)
                }
            }
        }
    }

    /**
     * In next version, this behavior will be embedded into jimmer
     */
    private class JimmerSimpleFetcher(
        private val propId: Int
    ) : DataFetcher<Any> {
        override fun get(environment: DataFetchingEnvironment): Any {
            val spi = environment.getSource<ImmutableSpi>()
            return spi.__get(propId)
        }
    }

    /**
     * In next version, this behavior will be embedded into jimmer
     */
    private class JimmerComplexFetcher(
        private val prop: ImmutableProp
    ) : DataFetcher<Any> {
        override fun get(env: DataFetchingEnvironment): Any {
            val dataLoader = env.dataLoaderRegistry.getDataLoader<Any, Any>(prop.toString())
                ?: throw IllegalStateException("No DataLoader for key '$prop'")
            return dataLoader.load(env.getSource())
        }
    }
}