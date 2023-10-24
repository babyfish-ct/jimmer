package org.babyfish.jimmer.sql.filter.common;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.model.filter.*;

public class FileFilter implements Filter<FileProps> {

    private static final ThreadLocal<Long> USER_ID_LOCAL = new ThreadLocal<>();

    public static void withUser(long userId, Runnable block) {
        Long oldUserId = USER_ID_LOCAL.get();
        USER_ID_LOCAL.set(userId);
        try {
            block.run();
        } finally {
            if (oldUserId != null) {
                USER_ID_LOCAL.set(oldUserId);
            } else {
                USER_ID_LOCAL.remove();
            }
        }
    }

    public static long currentUserId() {
        Long userId = USER_ID_LOCAL.get();
        if (userId == null) {
            throw new IllegalArgumentException("There is no current user id");
        }
        return userId;
    }

    @Override
    public void filter(FilterArgs<FileProps> args) {
        Long userId = currentUserId();
        FileProps table = args.getTable();
        AssociationTable<File, FileTableEx, User, UserTableEx> association =
                AssociationTable.of(FileTableEx.class, FileTableEx::users);
        args.where(
                args.createAssociationSubQuery(association)
                        .where(association.sourceId().eq(table.id()))
                        .where(association.targetId().eq(userId))
                        .exists()
        );
    }
}
