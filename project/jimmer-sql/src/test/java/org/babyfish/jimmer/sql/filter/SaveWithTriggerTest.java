package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.filter.common.FileFilter;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.model.filter.File;
import org.babyfish.jimmer.sql.model.filter.FileDraft;
import org.babyfish.jimmer.sql.model.filter.User;
import org.babyfish.jimmer.sql.trigger.AbstractTriggerTest;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SaveWithTriggerTest extends AbstractTriggerTest {

    private final JSqlClient sqlClient =
            getSqlClient(it -> {
                UserIdGenerator<?> idGenerator = this::autoId;
                it.setIdGenerator(idGenerator);
                it.addFilters(new FileFilter());
                it.setTriggerType(TriggerType.TRANSACTION_ONLY);
            });

    @Test
    public void testSaveWithOneToMany() {
        setAutoIds(File.class, 10000L);
        File file = FileDraft.$.produce(p -> {
            p.setId(8L);
            p.addIntoChildFiles(c -> c.setId(9L));
            p.addIntoChildFiles(c -> {
                c.setName("new_file");
            });
        });
        FileFilter.withUser(2L, () -> {
            executeAndExpectResult(
                    sqlClient.getEntities().saveCommand(file),
                    ctx -> {
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                            "from FILE tb_1_ " +
                                            "where " +
                                            "--->tb_1_.ID = ? " +
                                            "and " +
                                            "--->exists(" +
                                            "--->--->select 1 " +
                                            "--->--->from FILE_USER_MAPPING tb_2_ " +
                                            "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                            "--->)"
                            );
                            it.variables(8L, 2L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                            "from FILE tb_1_ where tb_1_.ID = ? and exists(" +
                                            "--->select 1 " +
                                            "--->from FILE_USER_MAPPING tb_2_ " +
                                            "--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                            ")"
                            );
                            it.variables(9L, 2L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                            "from FILE tb_1_ " +
                                            "where tb_1_.NAME = ? and tb_1_.PARENT_ID = ? and exists(" +
                                            "--->select 1 " +
                                            "--->from FILE_USER_MAPPING tb_3_ " +
                                            "--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                            ")"
                            );
                            it.variables("new_file", 8L, 2L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "insert into FILE(ID, NAME, PARENT_ID) values(?, ?, ?)"
                            );
                            it.variables(10000L, "new_file", 8L);
                        });
                        ctx.statement(it ->{
                            it.sql(
                                    "select tb_1_.ID " +
                                            "from FILE tb_1_ " +
                                            "where tb_1_.PARENT_ID = ? and tb_1_.ID not in (?, ?) and exists(" +
                                            "--->select 1 " +
                                            "--->from FILE_USER_MAPPING tb_3_ " +
                                            "--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                            ")"
                            );
                            it.variables(8L, 9L, 10000L, 2L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select FILE_ID, USER_ID from FILE_USER_MAPPING where FILE_ID in (?, ?)"
                            );
                            it.variables(11L, 12L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "delete from FILE_USER_MAPPING where (FILE_ID, USER_ID) in ((?, ?), (?, ?), (?, ?), (?, ?))"
                            );
                            it.variables(11L, 2L, 11L, 4L, 12L, 2L, 12L, 3L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID from FILE tb_1_ where tb_1_.PARENT_ID in (?, ?)"
                            );
                            it.variables(11L, 12L);
                        });
                        ctx.statement(it -> {
                            it.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID from FILE tb_1_ where tb_1_.ID in (?, ?)");
                            it.variables(11L, 12L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE where ID in (?, ?)");
                            it.variables(11L, 12L);
                        });
                        ctx.entity(it -> {
                            it.original("{\"id\":8,\"childFiles\":[{\"id\":9},{\"name\":\"new_file\"}]}");
                            it.modified(
                                    "{" +
                                            "--->\"id\":8," +
                                            "--->\"childFiles\":[" +
                                            "--->--->{\"id\":9}," +
                                            "--->--->{\"id\":10000,\"name\":\"new_file\",\"parent\":{\"id\":8}}" +
                                            "--->]" +
                                            "}"
                            );
                        });
                    }
            );
        });
        assertEvents(
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"id\":10000," +
                        "--->--->\"name\":\"new_file\"," +
                        "--->--->\"parent\":{\"id\":8}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.parent, sourceId=10000, detachedTargetId=null, attachedTargetId=8, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.childFiles, sourceId=8, detachedTargetId=null, attachedTargetId=10000, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=11, detachedTargetId=2, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=2, detachedTargetId=11, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=11, detachedTargetId=4, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=4, detachedTargetId=11, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=12, detachedTargetId=2, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=2, detachedTargetId=12, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=12, detachedTargetId=3, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=3, detachedTargetId=12, attachedTargetId=null, reason=null}",
                "Event{oldEntity={\"id\":11,\"name\":\"purge\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.parent, sourceId=11, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.childFiles, sourceId=8, detachedTargetId=11, attachedTargetId=null, reason=null}",
                "Event{oldEntity={\"id\":12,\"name\":\"ssh\",\"parent\":{\"id\":8}}, newEntity=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.parent, sourceId=12, detachedTargetId=8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.childFiles, sourceId=8, detachedTargetId=12, attachedTargetId=null, reason=null}"
        );
    }

    @Test
    public void testSaveWithManyToMany() {
        setAutoIds(User.class, 10000L);
        File file = FileDraft.$.produce(f -> {
            f.setId(20L);
            f.addIntoUsers(user -> user.setId(3L));
            f.addIntoUsers(user -> user.setName("Andrew"));
        });
        FileFilter.withUser(2L, () -> {
            executeAndExpectResult(
                    sqlClient.getEntities().saveCommand(file),
                    ctx -> {
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                            "from FILE tb_1_ " +
                                            "where tb_1_.ID = ? and exists(" +
                                            "--->select 1 " +
                                            "--->from FILE_USER_MAPPING tb_2_ " +
                                            "--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                            ")"
                            );
                            it.variables(20L, 2L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_TIME " +
                                            "from file_user tb_1_ " +
                                            "where tb_1_.NAME = ? and tb_1_.DELETED_TIME is null"
                            );
                            it.variables("Andrew");
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "insert into file_user(ID, NAME) values(?, ?)"
                            );
                            it.variables(10000L, "Andrew");
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID " +
                                            "from file_user tb_1_ " +
                                            "inner join FILE_USER_MAPPING tb_2_ on tb_1_.ID = tb_2_.USER_ID " +
                                            "where tb_2_.FILE_ID = ? and tb_1_.DELETED_TIME is null"
                            );
                            it.variables(20L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "delete from FILE_USER_MAPPING where (FILE_ID, USER_ID) in ((?, ?), (?, ?))"
                            );
                            it.variables(20L, 2L, 20L, 4L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "insert into FILE_USER_MAPPING(FILE_ID, USER_ID) values(?, ?), (?, ?)"
                            );
                            it.variables(20L, 3L, 20L, 10000L);
                        });
                        ctx.entity(it -> {
                            it.original("{\"id\":20,\"users\":[{\"id\":3},{\"name\":\"Andrew\"}]}");
                            it.modified(
                                    "{" +
                                            "--->\"id\":20," +
                                            "--->\"users\":[" +
                                            "--->--->{\"id\":3}," +
                                            "--->--->{\"id\":10000,\"name\":\"Andrew\"}" +
                                            "--->]" +
                                            "}"
                            );
                        });
                    }
            );
        });
        
        assertEvents(
                "Event{oldEntity=null, newEntity={\"id\":10000,\"name\":\"Andrew\"}, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=20, detachedTargetId=2, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=2, detachedTargetId=20, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=20, detachedTargetId=4, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=4, detachedTargetId=20, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=20, detachedTargetId=null, attachedTargetId=3, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=3, detachedTargetId=null, attachedTargetId=20, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.File.users, sourceId=20, detachedTargetId=null, attachedTargetId=10000, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.filter.User.files, sourceId=10000, detachedTargetId=null, attachedTargetId=20, reason=null}"
        );
    }
}
