package org.babyfish.jimmer.sql.filter.common;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.model.filter.FileProps;
import org.babyfish.jimmer.sql.model.filter.FileTable;
import org.babyfish.jimmer.sql.runtime.Initializer;

import java.util.List;

public class FileChildFilesInvalidator implements Initializer {

    @Override
    public void initialize(JSqlClient sqlClient) throws Exception {
        sqlClient.getTriggers().addAssociationListener(
                FileProps.USERS,
                e -> onChildFilesChange(e, sqlClient)
        );
    }

    private void onChildFilesChange(AssociationEvent e, JSqlClient sqlClient) {
        Cache<Long, ?> childNodesCache = sqlClient.getCaches().getPropertyCache(FileProps.CHILD_FILES);
        if (childNodesCache == null) {
            return;
        }

        FileTable table = FileTable.$;
        List<Long> parentIds = sqlClient
                .createQuery(table)
                .where(table.id().eq((Long)e.getSourceId()))
                .select(table.parent().id())
                .execute();
        for (Long parentId : parentIds) {
            if (parentId != null) {
                childNodesCache.delete(parentId);
            }
        }
    }
}
