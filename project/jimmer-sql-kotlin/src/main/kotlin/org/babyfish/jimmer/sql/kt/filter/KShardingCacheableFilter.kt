package org.babyfish.jimmer.sql.kt.filter

interface KShardingCacheableFilter<E: Any> : KCacheableFilter<E>, KShardingFilter<E>