package org.babyfish.jimmer.sql.example.graphql.cfg;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.TypedPropImpl;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * In next version, this behavior will be embedded into jimmer
 */
@Configuration
public class DataLoaderConfig {

    @SuppressWarnings("unchecked")
    public DataLoaderConfig(BatchLoaderRegistry registry, JSqlClient sqlClient) {
        for (ImmutableType type : sqlClient.getEntityManager().getAllTypes()) {
            if (type.isEntity()) {
                for (ImmutableProp prop : type.getProps().values()) {
                    if (prop.isReference(TargetLevel.ENTITY)) {
                        registry.forName(prop.toString()).registerMappedBatchLoader((sources, env) -> {
                            return Mono.just(
                                    sqlClient
                                            .getLoaders()
                                            .reference(
                                                    // Temporary solution, jimmer change graphql solution in next version
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
                                                    // Temporary solution, jimmer change graphql solution in next version
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
}
