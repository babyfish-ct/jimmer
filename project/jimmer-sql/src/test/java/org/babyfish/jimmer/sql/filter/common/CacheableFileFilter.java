package org.babyfish.jimmer.sql.filter.common;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.ParameterMaps;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.model.filter.FileProps;
import org.babyfish.jimmer.sql.model.filter.FileTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;

public class CacheableFileFilter extends FileFilter implements CacheableFilter<FileProps> {

    private JSqlClient sqlClient;

    @Override
    public SortedMap<String, Object> getParameters() {
        return ParameterMaps.of("userId", currentUserId());
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return false;
    }

    @Override
    public void initialize(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Nullable
    @Override
    public Collection<?> getAffectedSourceIds(@NotNull AssociationEvent e) {
        if (e.getImmutableProp() != FileProps.USERS.unwrap()) {
            return null;
        }
        return Collections.singleton(e.getSourceId());
    }
}
