package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.filter.common.FileFilter;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.model.filter.File;
import org.babyfish.jimmer.sql.model.filter.FileDraft;
import org.babyfish.jimmer.sql.model.filter.FileProps;
import org.babyfish.jimmer.sql.model.filter.User;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class SaveTest extends AbstractMutationTest {

    private final JSqlClient sqlClient =
            getSqlClient(it -> {
                UserIdGenerator<?> idGenerator = this::autoId;
                it.setIdGenerator(idGenerator);
                it.addFilters(new FileFilter());
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
                                            "--->tb_1_.ID = ?"
                            );
                            it.variables(9L);
                            it.queryReason(QueryReason.TARGET_NOT_TRANSFERABLE);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                            "from FILE tb_1_ " +
                                            "where (tb_1_.NAME, tb_1_.PARENT_ID) = (?, ?)"
                            );
                            it.variables("new_file", 8L);
                            it.queryReason(QueryReason.TARGET_NOT_TRANSFERABLE);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "insert into FILE(ID, NAME, PARENT_ID) values(?, ?, ?)"
                            );
                            it.variables(10000L, "new_file", 8L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID from FILE tb_1_ " +
                                            "inner join FILE tb_2_ on tb_1_.PARENT_ID = tb_2_.ID " +
                                            "inner join FILE tb_3_ on tb_2_.PARENT_ID = tb_3_.ID " +
                                            "where tb_3_.PARENT_ID = ? and tb_3_.ID not in (?, ?)"
                            );
                            it.variables(8L, 9L, 10000L);
                            it.queryReason(QueryReason.TOO_DEEP);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select FILE_ID, USER_ID from FILE_USER_MAPPING tb_1_ " +
                                            "inner join FILE tb_2_ on tb_1_.FILE_ID = tb_2_.ID " +
                                            "where exists(" +
                                            "--->select * " +
                                            "--->from FILE tb_3_ " +
                                            "--->where " +
                                            "--->--->tb_2_.PARENT_ID = tb_3_.ID " +
                                            "--->and " +
                                            "--->--->tb_3_.PARENT_ID = ? " +
                                            "--->and " +
                                            "--->--->tb_3_.ID not in (?, ?)" +
                                            ")"
                            );
                            it.variables(8L, 9L, 10000L);
                            it.queryReason(QueryReason.TOO_DEEP);
                        });
                        ctx.statement(it ->{
                            it.sql(
                                    "delete from FILE tb_1_ " +
                                            "where exists(" +
                                            "--->select * " +
                                            "--->from FILE tb_2_ " +
                                            "--->where " +
                                            "--->--->tb_1_.PARENT_ID = tb_2_.ID " +
                                            "--->and " +
                                            "--->--->tb_2_.PARENT_ID = ? " +
                                            "--->and " +
                                            "--->--->tb_2_.ID not in (?, ?)" +
                                            ")"
                            );
                            it.variables(8L, 9L, 10000L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "delete from FILE_USER_MAPPING tb_1_ " +
                                            "where exists (" +
                                            "--->select * " +
                                            "--->from FILE tb_2_ " +
                                            "--->where " +
                                            "--->--->tb_1_.FILE_ID = tb_2_.ID " +
                                            "--->and " +
                                            "--->--->tb_2_.PARENT_ID = ? " +
                                            "--->and " +
                                            "--->tb_2_.ID not in (?, ?)" +
                                            ")"
                            );
                            it.variables(8L, 9L, 10000L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "delete from FILE " +
                                            "where PARENT_ID = ? and ID not in (?, ?)"
                            );
                            it.variables(8L, 9L, 10000L);
                        });
                        ctx.totalRowCount(13);
                        ctx.rowCount(AffectedTable.of(File.class), 5);
                        ctx.rowCount(AffectedTable.of(FileProps.USERS), 8);
                        ctx.entity(it -> {
                            it.original("{\"id\":8,\"childFiles\":[{\"id\":9},{\"name\":\"new_file\"}]}");
                            it.modified(
                                    "{" +
                                            "--->\"id\":8," +
                                            "--->\"childFiles\":[" +
                                            "--->--->{\"id\":9,\"parent\":{\"id\":8}}," +
                                            "--->--->{\"id\":10000,\"name\":\"new_file\",\"parent\":{\"id\":8}}" +
                                            "--->]" +
                                            "}"
                            );
                        });
                    }
            );
        });
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
                                    "select tb_1_.ID, tb_1_.NAME " +
                                            "from file_user tb_1_ " +
                                            "where tb_1_.NAME = ? and tb_1_.DELETED_TIME is null"
                            );
                            it.variables("Andrew");
                            it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "insert into file_user(ID, NAME, DELETED_TIME) values(?, ?, ?)"
                            );
                            it.variables(10000L, "Andrew", new DbLiteral.DbNull(LocalDateTime.class));
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "delete from FILE_USER_MAPPING " +
                                            "where FILE_ID = ? and USER_ID not in (?, ?)"
                            );
                            it.variables(20L, 3L, 10000L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "merge into FILE_USER_MAPPING tb_1_ " +
                                            "using(values(?, ?)) tb_2_(FILE_ID, USER_ID) " +
                                            "on tb_1_.FILE_ID = tb_2_.FILE_ID and tb_1_.USER_ID = tb_2_.USER_ID " +
                                            "when not matched then insert(FILE_ID, USER_ID) " +
                                            "--->values(tb_2_.FILE_ID, tb_2_.USER_ID)"
                            );
                            it.batchVariables(0, 20L, 3L);
                            it.batchVariables(1, 20L, 10000L);
                        });
                        ctx.totalRowCount(6);
                        ctx.rowCount(AffectedTable.of(User.class), 1);
                        ctx.rowCount(AffectedTable.of(FileProps.USERS), 5);
                        ctx.entity(it -> {
                            it.original("{\"id\":20,\"users\":[{\"id\":3},{\"name\":\"Andrew\"}]}");
                            it.modified(
                                    "{" +
                                            "--->\"id\":20," +
                                            "--->\"users\":[" +
                                            "--->--->{\"id\":3}," +
                                            "--->--->{\"id\":10000,\"name\":\"Andrew\",\"deletedTime\":null}" +
                                            "--->]" +
                                            "}"
                            );
                        });
                    }
            );
        });
    }
}
