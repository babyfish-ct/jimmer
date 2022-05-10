package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorTable;
import org.babyfish.jimmer.sql.model.Gender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ScalarProviderTest extends AbstractQueryTest {

    @Test
    public void test() {
        executeAndExpect(
                AuthorTable.createQuery(getSqlClient(), (q, author) -> {
                    q.where(author.gender().eq(Gender.MALE));
                    return q.select(author);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1.ID, tb_1.FIRST_NAME, tb_1.LAST_NAME, tb_1.GENDER " +
                                    "from AUTHOR as tb_1 " +
                                    "where tb_1.GENDER = ?"
                    );
                    ctx.variables("M");
                    ctx.rows(it -> Assertions.assertEquals(5, it.size()));
                }
        );
    }
}