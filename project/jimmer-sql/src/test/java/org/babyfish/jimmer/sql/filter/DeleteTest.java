package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.filter.common.FileFilter;
import org.babyfish.jimmer.sql.model.filter.File;
import org.junit.jupiter.api.Test;

public class DeleteTest extends AbstractMutationTest {

    private final JSqlClient sqlClient =
            getSqlClient(it -> it.addFilters(new FileFilter()));

    @Test
    public void test() {
        FileFilter.withUser(2L, () -> {
            executeAndExpectResult(
                    sqlClient.getEntities().deleteCommand(File.class, 8L),
                    ctx -> {
                        ctx.statement(it -> {
                            it.sql("delete from FILE_USER_MAPPING where FILE_ID = ?");
                            it.variables(8L);
                        });
                        ctx.statement(it -> {
                            it.sql(
                                    "select tb_1_.ID " +
                                            "from FILE tb_1_ " +
                                            "where tb_1_.PARENT_ID = ? and exists(" +
                                            "--->select 1 " +
                                            "--->from FILE_USER_MAPPING tb_3_ " +
                                            "--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                            ")"
                            );
                            it.variables(8L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE_USER_MAPPING where FILE_ID in (?, ?, ?, ?, ?)");
                            it.variables(9L, 10L, 11L, 12L, 13L);
                        });
                        ctx.statement(it -> {
                            it.sql("select ID from FILE where PARENT_ID in (?, ?, ?, ?, ?)");
                            it.variables(9L, 10L, 11L, 12L, 13L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE where ID in (?, ?, ?, ?, ?)");
                            it.variables(9L, 10L, 11L, 12L, 13L);
                        });
                        ctx.statement(it -> {
                            it.sql("delete from FILE where ID = ?");
                            it.variables(8L);
                        });
                    }
            );
        });
    }
}
