package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.filter.common.FileFilter;
import org.babyfish.jimmer.sql.model.filter.File;
import org.babyfish.jimmer.sql.model.filter.FileProps;
import org.junit.jupiter.api.Test;

public class DeleteTest extends AbstractMutationTest {

    @Test
    public void test() {
        FileFilter.withUser(2L, () -> {
            executeAndExpectResult(
                    getSqlClient(it -> {
                        it.addFilters(new FileFilter());
                        it.setMaxCommandJoinCount(0);
                    }).getEntities().deleteCommand(File.class, 8L),
                    ctx -> {
                        ctx.statement(it -> {
                            it.sql("delete from FILE_USER_MAPPING where FILE_ID = ?");
                            it.variables(8L);
                        });
                        ctx.statement(it -> {
                            it.sql("select tb_1_.ID from FILE tb_1_ where tb_1_.PARENT_ID = ?");
                            it.variables(8L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID from FILE tb_1_ inner join FILE tb_2_ on tb_1_.PARENT_ID = tb_2_.ID where tb_2_.ID in (?, ?, ?, ?, ?)"
                            );
                            it.variables(9L, 10L, 11L, 12L, 13L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select FILE_ID, USER_ID " +
                                            "from FILE_USER_MAPPING " +
                                            "where FILE_ID in (?, ?, ?, ?, ?)"
                            );
                            it.variables(9L, 10L, 11L, 12L, 13L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "delete from FILE_USER_MAPPING " +
                                            "where FILE_ID = ? and USER_ID = ?"
                            );
                            it.variables(9L, 2L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE where ID in (?, ?, ?, ?, ?)");
                            it.variables(9L, 10L, 11L, 12L, 13L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE where ID = ?");
                            it.variables(8L);
                        });
                        ctx.rowCount(AffectedTable.of(File.class), 6);
                        ctx.rowCount(AffectedTable.of(FileProps.USERS), 13);
                    }
            );
        });
    }
}
