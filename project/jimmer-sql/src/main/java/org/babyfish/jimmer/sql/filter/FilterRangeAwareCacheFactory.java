package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.filter.impl.FilterRangeAware;

public interface FilterRangeAwareCacheFactory extends CacheFactory, FilterRangeAware {
}
