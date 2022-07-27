package org.babyfish.jimmer.sql.kt.fetcher

import kotlin.reflect.KClass

@JvmInline
value class FetcherCreator<T: Any> internal constructor(val type: KClass<T>)

fun <T: Any> newFetcher(type: KClass<T>): FetcherCreator<T> =
    FetcherCreator(type)