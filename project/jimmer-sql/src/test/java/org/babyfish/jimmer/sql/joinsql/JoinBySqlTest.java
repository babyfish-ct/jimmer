package org.babyfish.jimmer.sql.joinsql;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.joinsql.CategoryTable;
import org.babyfish.jimmer.sql.model.joinsql.PostTable;
import org.junit.jupiter.api.Test;

public class JoinBySqlTest extends AbstractQueryTest {

    @Test
    public void testJoin() {

        PostTable table = PostTable.$;

        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.asTableEx().categories().name().eq("category-1"))
                        .select(table.name())
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.NAME " +
                                    "from POST tb_1_ " +
                                    "inner join CATEGORY tb_2_ " +
                                    "--->on contains_id(tb_1_.category_ids, tb_2_.id) " +
                                    "where tb_2_.NAME = ?"
                    );
                    ctx.rows("[\"post-1\",\"post-2\"]");
                }
        );
    }

    @Test
    public void testInverseJoin() {

        CategoryTable table = CategoryTable.$;

        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.asTableEx().posts().name().eq("post-1"))
                        .select(table.name())
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.NAME " +
                                    "from CATEGORY tb_1_ " +
                                    "inner join POST tb_2_ " +
                                    "--->on contains_id(tb_2_.category_ids, tb_1_.id) " +
                                    "where tb_2_.NAME = ?"
                    );
                    ctx.rows("[\"category-1\",\"category-2\"]");
                }
        );
    }

    @Test
    public void testJoinById() {

        PostTable table = PostTable.$;

        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.asTableEx().categories().id().eq(1L))
                        .select(table.name())
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.NAME " +
                                    "from POST tb_1_ " +
                                    "inner join CATEGORY tb_2_ " +
                                    "--->on contains_id(tb_1_.category_ids, tb_2_.id) " +
                                    "where tb_2_.ID = ?"
                    );
                    ctx.rows("[\"post-1\",\"post-2\"]");
                }
        );
    }

    @Test
    public void testInverseJoinById() {

        CategoryTable table = CategoryTable.$;

        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.asTableEx().posts().id().eq(2L))
                        .select(table.name())
                        .distinct(),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.NAME " +
                                    "from CATEGORY tb_1_ " +
                                    "inner join POST tb_2_ " +
                                    "--->on contains_id(tb_2_.category_ids, tb_1_.id) " +
                                    "where tb_2_.ID = ?"
                    );
                    ctx.rows("[\"category-1\",\"category-2\"]");
                }
        );
    }
}
