package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.model.inheritance.Administrator;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorFetcher;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorMetadataFetcher;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorTable;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.junit.jupiter.api.Test;

public class OneToOneTest extends AbstractCachedLoaderTest {

    @Test
    public void test() {
        AdministratorTable table = AdministratorTable.$;
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            executeAndExpect(
                    getCachedSqlClient()
                            .createQuery(table)
                            .where(table.name().eq("a_1"))
                            .select(
                                    table.fetch(
                                            AdministratorFetcher.$
                                                    .name()
                                                    .metadata(
                                                            AdministratorMetadataFetcher.$.name()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME from ADMINISTRATOR tb_1_ where tb_1_.NAME = ? and tb_1_.DELETED <> ?"
                        );
                        if (useSql) {
                            ctx.statement(1).sql(
                                    "select tb_1_.ID from ADMINISTRATOR_METADATA tb_1_ where tb_1_.ADMINISTRATOR_ID = ? and tb_1_.DELETED <> ?"
                            );
                            ctx.statement(2).sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME, tb_1_.EMAIL, tb_1_.WEBSITE, tb_1_.ADMINISTRATOR_ID from ADMINISTRATOR_METADATA tb_1_ where tb_1_.ID = ? and tb_1_.DELETED <> ?"
                            );
                        }
                        ctx.rows(
                                "[{\"name\":\"a_1\",\"metadata\":{\"name\":\"am_1\",\"id\":10},\"id\":1}]"
                        );
                    }
            );
        }
    }

    @Test
    public void testIssue706() {
        AdministratorTable table = AdministratorTable.$;
        for (int i = 0; i < 1; i++) {
            boolean useSql = i == 0;
            executeAndExpect(
                    getCachedSqlClient()
                            .filters(it -> it.setBehavior(Administrator.class, LogicalDeletedBehavior.IGNORED))
                            .createQuery(table)
                            .where(table.name().eq("a_-1"))
                            .select(
                                    table.fetch(
                                            AdministratorFetcher.$
                                                    .name()
                                                    .metadata(
                                                            AdministratorMetadataFetcher.$.name()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from ADMINISTRATOR tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                        if (useSql) {
                            ctx.statement(1).sql(
                                    "select tb_1_.ID " +
                                            "from ADMINISTRATOR_METADATA tb_1_ " +
                                            "where tb_1_.ADMINISTRATOR_ID = ? and tb_1_.DELETED <> ?"
                            );
                        }
                        ctx.rows(
                                "[{\"name\":\"a_-1\",\"metadata\":null,\"id\":-1}]"
                        );
                    }
            );
        }
    }
}
