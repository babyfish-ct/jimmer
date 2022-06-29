package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.IdAndNameEntityTable;
import org.junit.jupiter.api.Test;

public class InheritanceTest extends AbstractQueryTest {

    @Test
    public void test() {
        executeAndExpect(
                getSqlClient().createQuery(IdAndNameEntityTable.class, (q, entity) -> {
                    return q.select(entity);
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_");
                }
        );
    }
}
