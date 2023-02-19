package org.babyfish.jimmer.sql.example.graphql.cfg;

import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.dataloader.DataLoader;
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
    public RuntimeWiringConfigurer runtimeWiringConfigurer(EntityManager entityManager) {
        return wiringBuilder -> {
            wiringBuilder
                    .scalar(ExtendedScalars.GraphQLLong)
                    .scalar(ExtendedScalars.GraphQLBigDecimal)
                    .scalar(GRAPHQL_LOCAL_DATE_TIME);
            registerJimmerDataFetchers(wiringBuilder, entityManager);
        };
    }

    private static void registerJimmerDataFetchers(
            RuntimeWiring.Builder wiringBuilder,
            EntityManager entityManager) {
        for (ImmutableType type : entityManager.getAllTypes()) {
            if (type.isEntity()) {
                TypeRuntimeWiring.Builder typeBuilder = TypeRuntimeWiring
                        .newTypeWiring(type.getJavaClass().getSimpleName());
                for (ImmutableProp prop : type.getProps().values()) {
                    if (prop.isAssociation(TargetLevel.ENTITY) || prop.hasTransientResolver()) {
                        typeBuilder.dataFetcher(prop.getName(), new JimmerComplexFetcher(prop));
                    } else {
                        typeBuilder.dataFetcher(prop.getName(), new JimmerSimpleFetcher(prop.getId()));
                    }
                }
                wiringBuilder.type(typeBuilder);
            }
        }
    }

    private static class JimmerSimpleFetcher implements DataFetcher<Object> {

        private final int propId;

        JimmerSimpleFetcher(int propId) {
            this.propId = propId;
        }

        @Override
        public Object get(DataFetchingEnvironment environment) throws Exception {
            ImmutableSpi spi = environment.getSource();
            return spi.__get(propId);
        }
    }

    private static class JimmerComplexFetcher implements DataFetcher<Object> {

        private final ImmutableProp prop;

        JimmerComplexFetcher(ImmutableProp prop) {
            this.prop = prop;
        }

        @Override
        public Object get(DataFetchingEnvironment env) throws Exception {
            DataLoader<?, ?> dataLoader = env.getDataLoaderRegistry().getDataLoader(prop.toString());
            if (dataLoader == null) {
                throw new IllegalStateException("No DataLoader for key '" + prop + "'");
            }
            return dataLoader.load(env.getSource());
        }
    }
}
