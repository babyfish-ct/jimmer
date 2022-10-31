package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class TransientResolverTest extends AbstractQueryTest {

    @Test
    public void test() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .allScalarFields()
                                            .avgPrice()
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.STORE_ID, coalesce(avg(tb_1_.PRICE), ?) " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?) " +
                                    "group by tb_1_.STORE_ID"
                    ).variables(BigDecimal.ZERO, oreillyId, manningId);
                    it.rows(
                            "[{" +
                                    "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->\"name\":\"O'REILLY\"," +
                                    "--->\"website\":null," +
                                    "--->\"version\":0," +
                                    "--->\"avgPrice\":58.500000000000" +
                                    "},{" +
                                    "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->\"name\":\"MANNING\"," +
                                    "--->\"website\":null," +
                                    "--->\"version\":0," +
                                    "--->\"avgPrice\":80.333333333333" +
                                    "}" +
                                    "]"
                    );
                }
        );
    }
}
