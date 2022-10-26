package org.babyfish.jimmer.sql.kt.filter

interface KCoerciveCacheableFilter<E: Any> : KCacheableFilter<E>, KCoerciveFilter<E>