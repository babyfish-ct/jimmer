package org.babyfish.jimmer.spring.cfg;

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.TypedPropImpl;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.dataloader.DataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import reactor.core.publisher.Mono;

import java.util.Map;

@ConditionalOnClass({GraphQL.class, GraphQlSource.class})
public class JimmerSpringGraphQLAutoConfiguration {

    @SuppressWarnings("unchecked")
    public JimmerSpringGraphQLAutoConfiguration(
            BatchLoaderRegistry registry,
            @Autowired(required = false) JSqlClient jSqlClient,
            @Autowired(required = false) KSqlClient kSqlClient
    ) {
        JSqlClient sqlClient = jSqlClient != null ? jSqlClient : kSqlClient.getJavaClient();
        for (ImmutableType type : sqlClient.getEntityManager().getAllTypes()) {
            if (type.isEntity()) {
                for (ImmutableProp prop : type.getProps().values()) {
                    if (prop.isReference(TargetLevel.ENTITY)) {
                        registry.forName(prop.toString()).registerMappedBatchLoader((sources, env) -> {
                            return Mono.just(
                                    sqlClient
                                            .getLoaders()
                                            .reference(
                                                    new TypedPropImpl.Reference<>(prop)
                                            )
                                            .batchLoad(sources)
                            );
                        });
                    } else if (prop.isReferenceList(TargetLevel.ENTITY)) {
                        registry.forName(prop.toString()).registerMappedBatchLoader((sources, env) -> {
                            return Mono.just(
                                    (Map<Object, Object>) (Map<?, ?>)sqlClient
                                            .getLoaders()
                                            .list(
                                                    new TypedPropImpl.ReferenceList<>(prop)
                                            )
                                            .batchLoad(sources)
                            );
                        });
                    } else if (prop.hasTransientResolver()) {
                        registry.forName(prop.toString()).registerMappedBatchLoader((sources, env) -> {
                            return Mono.just(
                                    sqlClient
                                            .getLoaders()
                                            .value(
                                                    // Temporary solution, jimmer change graphql solution in next version
                                                    new TypedPropImpl.Scalar<>(prop)
                                            )
                                            .batchLoad(sources)
                            );
                        });
                    }
                }
            }
        }
    }

    @Bean
    public RuntimeWiringConfigurer jimmerRuntimeWiringConfigurer(EntityManager entityManager) {
        return wiringBuilder -> {
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

    /**
     * In next version, this behavior will be embedded into jimmer
     */
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
