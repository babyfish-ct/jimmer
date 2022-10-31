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
                getLambdaClient().createQuery(AuthorTable.class, (q, author) -> {
                    q.where(author.gender().eq(Gender.MALE));
                    return q.select(author);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_ " +
                                    "where tb_1_.GENDER = ?"
                    );
                    ctx.variables("M");
                    ctx.rows(it -> Assertions.assertEquals(4, it.size()));
                }
        );
    }
}