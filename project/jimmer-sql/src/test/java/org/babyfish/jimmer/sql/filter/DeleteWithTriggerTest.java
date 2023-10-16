package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.filter.common.FileFilter;
import org.babyfish.jimmer.sql.model.filter.File;
import org.babyfish.jimmer.sql.trigger.AbstractTriggerTest;
import org.junit.jupiter.api.Test;

public class DeleteWithTriggerTest extends AbstractTriggerTest {

    private final JSqlClient sqlClient =
            getSqlClient(it -> it.addFilters(new FileFilter()));

    @Test
    public void test() {
        FileFilter.withUser(2L, () -> {
            executeAndExpectResult(
                    sqlClient.getEntities().deleteCommand(File.class, 8L),
                    ctx -> {
                        ctx.statement(it -> {
                            it.sql("select FILE_ID, USER_ID from FILE_USER_MAPPING where FILE_ID = ?");
                            it.variables(8L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE_USER_MAPPING where (FILE_ID, USER_ID) in ((?, ?), (?, ?), (?, ?))");
                            it.variables(8L, 2L, 8L, 3L, 8L, 4L);
                        });
                        ctx.statement(it -> {
                            it.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID from FILE tb_1_ where tb_1_.PARENT_ID = ?");
                            it.variables(8L);
                        });
                        ctx.statement(it -> {
                            it.sql("select FILE_ID, USER_ID from FILE_USER_MAPPING where FILE_ID in (?, ?, ?, ?, ?)");
                            it.variables(9L, 10L, 11L, 12L, 13L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE_USER_MAPPING where (FILE_ID, USER_ID) in " +
                                    "((?, ?), (?, ?), (?, ?), (?, ?), (?, ?), (?, ?), (?, ?), (?, ?), (?, ?), (?, ?))");
                            it.variables(9L, 2L, 9L, 3L, 10L, 3L, 10L, 4L, 11L, 2L, 11L, 4L, 12L, 2L, 12L, 3L, 13L, 3L, 13L, 4L);
                        });
                        ctx.statement(it -> {
                            it.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID from FILE tb_1_ where tb_1_.PARENT_ID in (?, ?, ?, ?, ?)");
                            it.variables(9L, 10L, 11L, 12L, 13L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE where ID in (?, ?, ?, ?, ?)");
                            it.variables(9L, 10L, 11L, 12L, 13L);
                        });
                        ctx.statement(it -> {
                            it.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID from FILE tb_1_ where tb_1_.ID = ?");
                            it.variables(8L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE where ID = ?");
                            it.variables(8L);
                        });
                    }
            );
        });

        assertEvents(
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=8, detachedTargetId=2, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=2, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=8, detachedTargetId=3, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=3, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=8, detachedTargetId=4, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=4, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=9, detachedTargetId=2, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=2, detachedTargetId=9, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=9, detachedTargetId=3, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=3, detachedTargetId=9, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=10, detachedTargetId=3, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=3, detachedTargetId=10, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=10, detachedTargetId=4, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=4, detachedTargetId=10, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=11, detachedTargetId=2, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=2, detachedTargetId=11, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=11, detachedTargetId=4, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=4, detachedTargetId=11, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=12, detachedTargetId=2, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=2, detachedTargetId=12, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=12, detachedTargetId=3, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=3, detachedTargetId=12, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=13, detachedTargetId=3, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=3, detachedTargetId=13, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=13, detachedTargetId=4, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=4, detachedTargetId=13, attachedTargetId=null, reason=null}",
                "Event{oldEntity={\"id\":9,\"name\":\"ipconfig\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.parent, sourceId=9, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.childFiles, sourceId=8, detachedTargetId=9, attachedTargetId=null, reason=null}",
                "Event{oldEntity={\"id\":10,\"name\":\"mtree\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.parent, sourceId=10, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.childFiles, sourceId=8, detachedTargetId=10, attachedTargetId=null, reason=null}",
                "Event{oldEntity={\"id\":11,\"name\":\"purge\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.parent, sourceId=11, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.childFiles, sourceId=8, detachedTargetId=11, attachedTargetId=null, reason=null}",
                "Event{oldEntity={\"id\":12,\"name\":\"ssh\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.parent, sourceId=12, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.childFiles, sourceId=8, detachedTargetId=12, attachedTargetId=null, reason=null}",
                "Event{oldEntity={\"id\":13,\"name\":\"tcpctl\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.parent, sourceId=13, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.childFiles, sourceId=8, detachedTargetId=13, attachedTargetId=null, reason=null}",
                "Event{oldEntity={\"id\":8,\"name\":\"sbin\",\"parent\":{\"id\":1}}, newEntity=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.parent, sourceId=8, detachedTargetId=1, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.childFiles, sourceId=1, detachedTargetId=8, attachedTargetId=null, reason=null}"
        );
    }
}
