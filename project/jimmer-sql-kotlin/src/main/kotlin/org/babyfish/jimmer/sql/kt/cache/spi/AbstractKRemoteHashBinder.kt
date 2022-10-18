package org.babyfish.jimmer.sql.kt.cache.spi

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteHashBinder
import java.time.Duration

abstract class AbstractKRemoteHashBinder<K, V>(
    objectMapper: ObjectMapper?,
    type: ImmutableType?,
    prop: ImmutableProp?,
    duration: Duration,
    randomPercent: Int
) : AbstractRemoteHashBinder<K, V>(
    objectMapper,
    type,
    prop,
    duration,
    randomPercent
) {

    abstract override fun read(keys: Collection<String>, hashKey: String): List<ByteArray?>

    abstract override fun write(map: Map<String, ByteArray>, hashKey: String)

    abstract override fun delete(keys: Collection<String>)
}