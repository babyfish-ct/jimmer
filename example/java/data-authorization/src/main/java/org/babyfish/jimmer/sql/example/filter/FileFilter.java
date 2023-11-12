package org.babyfish.jimmer.sql.example.filter;

import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.cache.ParameterMaps;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.example.Context;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;

public class FileFilter implements CacheableFilter<FileProps>, Context {

    @Override
    public void filter(FilterArgs<FileProps> args) {

        FileProps table = args.getTable();

        AssociationTable<File, FileTableEx, User, UserTableEx> mappingTable =
                AssociationTable.of(FileTableEx.class, FileTableEx::authorizedUsers);

        args.where(
                args.createAssociationSubQuery(mappingTable)
                        .where(mappingTable.sourceId().eq(table.id()))
                        .where(mappingTable.targetId().eq(USER_SERVICE.currentUser().id()))
                        .exists()
        );
    }

    @Override
    public SortedMap<String, Object> getParameters() {
        return ParameterMaps.of("userId", USER_SERVICE.currentUser().id());
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return false;
    }

    @Nullable
    @Override
    public Collection<?> getAffectedSourceIds(@NotNull AssociationEvent e) {
        if (e.getImmutableProp() == FileTableEx.AUTHORIZED_USERS.unwrap()) {
            return Collections.singletonList(e.getSourceId());
        }
        return null;
    }
}
