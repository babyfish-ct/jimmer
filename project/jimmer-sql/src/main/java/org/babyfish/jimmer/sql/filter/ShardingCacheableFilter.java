package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Props;

public interface ShardingCacheableFilter<P extends Props> extends CacheableFilter<P>, ShardingFilter<P> {
}
