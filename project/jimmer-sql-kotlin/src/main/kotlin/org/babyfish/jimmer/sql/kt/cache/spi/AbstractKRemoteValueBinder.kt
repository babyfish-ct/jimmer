package org.babyfish.jimmer.sql.kt.cache.spi

import org.babyfish.jimmer.jackson.codec.JsonCodec
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.CacheTracker
import org.babyfish.jimmer.sql.cache.RemoteKeyPrefixProvider
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteValueBinder
import java.time.Duration

abstract class AbstractKRemoteValueBinder<K, V>(
    type: ImmutableType?,
    prop: ImmutableProp?,
    tracker: CacheTracker?,
    jsonCodec: JsonCodec<*>,
    keyPrefixProvider: RemoteKeyPrefixProvider?,
    duration: Duration,
    randomPercent: Int
) : AbstractRemoteValueBinder<K, V>(
    type,
    prop,
    tracker,
    jsonCodec,
    keyPrefixProvider,
    duration,
    randomPercent
) {

    abstract override fun read(keys: Collection<String>): List<ByteArray?>

    abstract override fun write(map: Map<String, ByteArray>)

    abstract override fun deleteAllSerializedKeys(serializedKeys: List<String>)
}
