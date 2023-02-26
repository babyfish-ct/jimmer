package org.babyfish.jimmer.example.kt.graphql.cfg

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.TargetLevel
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.dataloader.BatchLoaderEnvironment
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.BatchLoaderRegistry
import reactor.core.publisher.Mono
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

/**
 * In next version, this behavior will be embedded into jimmer
 */
@Configuration
class DataLoaderConfig(registry: BatchLoaderRegistry, sqlClient: KSqlClient) {
    init {
        for (type in sqlClient.entityManager.allTypes) {
            if (type.isEntity) {
                for (prop in type.props.values) {
                    if (prop.isReference(TargetLevel.ENTITY)) {
                        registry.forName<Any, Any>(prop.toString())
                            .registerMappedBatchLoader { sources: Set<Any>, env: BatchLoaderEnvironment ->
                                Mono.just(
                                    sqlClient
                                        .loaders
                                        .reference(prop.toKotlinProp())
                                        .batchLoad(sources)
                                )
                            }
                    } else if (prop.isReferenceList(TargetLevel.ENTITY)) {
                        registry.forName<Any, Any>(prop.toString())
                            .registerMappedBatchLoader { sources: Set<Any>, env: BatchLoaderEnvironment ->
                                Mono.just(
                                    sqlClient
                                        .loaders
                                        .list(prop.toKotlinProp<Any, List<Any>>())
                                        .batchLoad(sources)
                                )
                            }
                    } else if (prop.hasTransientResolver()) {
                        registry.forName<Any, Any>(prop.toString())
                            .registerMappedBatchLoader { sources: Set<Any>, env: BatchLoaderEnvironment ->
                                Mono.just(
                                    sqlClient
                                        .loaders
                                        .value(prop.toKotlinProp())
                                        .batchLoad(sources)
                                )
                            }
                    }
                }
            }
        }
    }

    companion object {

        // Temporary solution, jimmer will change graphql solution is next version
        @Suppress("UNCHECKED_CAST")
        fun <S, T> ImmutableProp.toKotlinProp(): KProperty1<S, T> =
            declaringType.javaClass.kotlin.declaredMemberProperties.first {
                it.name == name
            } as KProperty1<S, T>
    }
}