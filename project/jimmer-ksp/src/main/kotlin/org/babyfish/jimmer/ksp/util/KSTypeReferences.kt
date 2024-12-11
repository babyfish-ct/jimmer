package org.babyfish.jimmer.ksp.util

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private val cache: ConcurrentMap<KSTypeReference, KSType> = ConcurrentHashMap()

fun KSTypeReference.fastResolve() : KSType =
    cache.computeIfAbsent(this) {
        it.resolve()
    }