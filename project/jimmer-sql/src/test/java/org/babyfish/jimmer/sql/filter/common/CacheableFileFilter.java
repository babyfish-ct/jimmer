package org.babyfish.jimmer.sql.filter.common;

import org.babyfish.jimmer.sql.cache.ParameterMaps;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.model.filter.FileProps;

import java.util.SortedMap;

public class CacheableFileFilter extends FileFilter implements CacheableFilter<FileProps> {

    @Override
    public SortedMap<String, Object> getParameters() {
        return ParameterMaps.of("userId", currentUserId());
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return false;
    }
}
