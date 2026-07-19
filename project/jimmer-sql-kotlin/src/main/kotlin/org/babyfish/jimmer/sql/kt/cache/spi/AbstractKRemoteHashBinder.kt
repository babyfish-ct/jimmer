package org.babyfish.jimmer.sql.kt.cache.spi

import org.babyfish.jimmer.jackson.codec.JsonCodec
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.CacheTracker
import org.babyfish.jimmer.sql.cache.RemoteKeyPrefixProvider
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteHashBinder
import java.time.Duration

abstract class AbstractKRemoteHashBinder<K, V>(
    type: ImmutableType?,
    prop: ImmutableProp?,
    tracker: CacheTracker?,
    jsonCodec: JsonCodec<*>,
    keyPrefixProvider: RemoteKeyPrefixProvider?,
    duration: Duration,
    randomPercent: Int
) : AbstractRemoteHashBinder<K, V>(
    type,
    prop,
    tracker,
    jsonCodec,
    keyPrefixProvider,
    duration,
    randomPercent
) {

    abstract override fun read(keys: Collection<String>, hashKey: String): List<ByteArray?>

    abstract override fun write(map: Map<String, ByteArray>, hashKey: String)

    abstract override fun deleteAllSerializedKeys(serializedKeys: List<String>)
}
