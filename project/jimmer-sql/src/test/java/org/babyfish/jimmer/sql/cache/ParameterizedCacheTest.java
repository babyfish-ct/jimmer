package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.model.inheritance.NamedEntity;
import org.babyfish.jimmer.sql.model.inheritance.NamedEntityColumns;
import org.babyfish.jimmer.sql.model.inheritance.NamedEntityProps;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ParameterizedCacheTest extends AbstractQueryTest {

    private static final UndeletedFilter UNDELETED_FILTER = new UndeletedFilter();

    private static final DeletedFilter DELETED_FILTER = new DeletedFilter();

    private JSqlClient sqlClient;

    @BeforeEach
    public void initialize() {
        sqlClient = getSqlClient(it -> {

        });
    }

    private static class UndeletedFilter implements CacheableFilter<NamedEntityColumns> {

        @Override
        public void filter(FilterArgs<NamedEntityColumns> args) {
            args.where(args.getTable().deleted().eq(false));
        }

        @Override
        public NavigableMap<String, Object> getParameters() {
            NavigableMap<String, Object> map = new TreeMap<>();
            map.put("deleted", false);
            return map;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return NamedEntity.class.isAssignableFrom(e.getImmutableType().getJavaClass()) &&
                    e.getUnchangedFieldRef(NamedEntityProps.DELETED) == null;
        }
    }

    private static class DeletedFilter implements CacheableFilter<NamedEntityColumns> {

        @Override
        public void filter(FilterArgs<NamedEntityColumns> args) {
            args.where(args.getTable().deleted().eq(true));
        }

        @Override
        public NavigableMap<String, Object> getParameters() {
            NavigableMap<String, Object> map = new TreeMap<>();
            map.put("deleted", true);
            return map;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return NamedEntity.class.isAssignableFrom(e.getImmutableType().getJavaClass()) &&
                    e.getUnchangedFieldRef(NamedEntityProps.DELETED) == null;
        }
    }
}
