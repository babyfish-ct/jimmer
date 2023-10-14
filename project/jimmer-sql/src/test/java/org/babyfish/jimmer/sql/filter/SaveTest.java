package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.filter.common.FileFilter;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.model.filter.File;
import org.babyfish.jimmer.sql.model.filter.FileDraft;
import org.babyfish.jimmer.sql.model.filter.User;
import org.junit.jupiter.api.Test;

public class SaveTest extends AbstractMutationTest {

    private final JSqlClient sqlClient =
            getSqlClient(it -> {
                UserIdGenerator idGenerator = this::autoId;
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
                                    "update FILE set PARENT_ID = ? where ID = ?"
                            );
                            it.variables(8L, 9L);
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
                                    "delete from FILE_USER_MAPPING where FILE_ID in (?, ?)"
                            );
                            it.variables(11L, 12L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID " +
                                            "from FILE tb_1_ " +
                                            "where tb_1_.PARENT_ID in (?, ?) and exists(" +
                                            "--->select 1 " +
                                            "--->from FILE_USER_MAPPING tb_3_ " +
                                            "--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                            ")"
                            );
                            it.variables(11L, 12L, 2L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "delete from FILE where ID in (?, ?)"
                            );
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
                                    "select tb_1_.ID, tb_1_.NAME " +
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
    }
}
