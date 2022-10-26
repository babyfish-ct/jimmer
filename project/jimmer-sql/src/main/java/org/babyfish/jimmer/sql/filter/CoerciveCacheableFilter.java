package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Props;

public interface CoerciveCacheableFilter<P extends Props> extends CacheableFilter<P>, CoerciveFilter<P> {
}
