package org.babyfish.jimmer.spring.cfg;

import graphql.GraphQL;
import graphql.language.*;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.TypedPropImpl;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.dataloader.DataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@ConditionalOnClass({GraphQL.class, GraphQlSource.class})
public class JimmerSpringGraphQLAutoConfiguration {

    @SuppressWarnings("unchecked")
    public JimmerSpringGraphQLAutoConfiguration(
            BatchLoaderRegistry registry,
            @Autowired(required = false) JSqlClient jSqlClient,
            @Autowired(required = false) KSqlClient kSqlClient
    ) {
        JSqlClientImplementor sqlClient = sqlClient(jSqlClient, kSqlClient);
        for (ImmutableType type : sqlClient.getEntityManager().getAllTypes(sqlClient.getMicroServiceName())) {
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
    public RuntimeWiringConfigurer jimmerRuntimeWiringConfigurer(
            @Autowired(required = false) JSqlClient jSqlClient,
            @Autowired(required = false) KSqlClient kSqlClient
    ) {
        JSqlClientImplementor sqlClient = sqlClient(jSqlClient, kSqlClient);
        return wiringBuilder -> {
            registerJimmerDataFetchers(wiringBuilder, sqlClient);
        };
    }

    private static void registerJimmerDataFetchers(
            RuntimeWiring.Builder wiringBuilder,
            JSqlClientImplementor sqlClient
    ) {
        for (ImmutableType type : sqlClient.getEntityManager().getAllTypes(sqlClient.getMicroServiceName())) {
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

    private static JSqlClientImplementor sqlClient(
            JSqlClient jSqlClient,
            KSqlClient kSqlClient
    ) {
        return (JSqlClientImplementor) (
                jSqlClient != null ?
                        jSqlClient :
                        kSqlClient.getJavaClient()
        );
    }

    private static class JimmerSimpleFetcher implements DataFetcher<Object> {

        private final PropId propId;

        JimmerSimpleFetcher(PropId propId) {
            this.propId = propId;
        }

        @Override
        public Object get(DataFetchingEnvironment env) throws Exception {
            ImmutableSpi spi = env.getSource();
            return spi.__get(propId);
        }
    }

    private static class JimmerComplexFetcher implements DataFetcher<Object> {

        private final ImmutableProp prop;

        JimmerComplexFetcher(ImmutableProp prop) {
            this.prop = prop;
        }

        @Override
        public Object get(DataFetchingEnvironment env) {
            ImmutableSpi spi = env.getSource();
            if (spi.__isLoaded(prop.getId())) {
                Object value = spi.__get(prop.getId());
                if (value == null) {
                    return null;
                }
                if (!new UnloadedContext(env).isUnloaded(value)) {
                    return value;
                }
            }
            DataLoader<?, ?> dataLoader = env.getDataLoaderRegistry().getDataLoader(prop.toString());
            if (dataLoader == null) {
                throw new IllegalStateException("No DataLoader for key '" + prop + "'");
            }
            return dataLoader.load(env.getSource());
        }
    }

    private static class UnloadedContext {

        private final DataFetchingEnvironment env;

        private UnloadedContext(DataFetchingEnvironment env) {
            this.env = env;
        }

        boolean isUnloaded(Object value) {
            SelectionSet selectionSet = env.getMergedField().getSingleField().getSelectionSet();
            if (value instanceof List<?>) {
                for (Object e : (List<?>)value) {
                    if (isUnloaded((ImmutableSpi) e, selectionSet)) {
                        return true;
                    }
                }
            } else {
                return isUnloaded((ImmutableSpi) value, selectionSet);
            }
            return false;
        }

        boolean isUnloaded(ImmutableSpi spi, SelectionSet selectionSet) {
            for (Selection selection : selectionSet.getSelections()) {
                if (selection instanceof FragmentSpread) {
                    if (isUnloaded(spi, (FragmentSpread) selection)) {
                        return true;
                    }
                } else if (selection instanceof InlineFragment) {
                    if (isUnloaded(spi, (InlineFragment) selection)) {
                        return true;
                    }
                } else if (isUnloaded(spi, (Field) selection)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isUnloaded(ImmutableSpi spi, Field field) {
            ImmutableProp prop = spi.__type().getProps().get(field.getName());
            if (prop == null) {
                return false;
            }
            return !spi.__isLoaded(prop.getId());
        }

        private boolean isUnloaded(ImmutableSpi spi, FragmentSpread fragmentSpread) {
            FragmentDefinition definition = env.getFragmentsByName().get(fragmentSpread.getName());
            return definition != null && isUnloaded(spi, definition.getSelectionSet());
        }

        private boolean isUnloaded(ImmutableSpi spi, InlineFragment inlineFragment) {
            return isUnloaded(spi, inlineFragment.getSelectionSet());
        }
    }
}
