package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.DataLoader;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class ManyToManyWithoutCacheTest extends AbstractQueryTest {

    @Test
    public void loadTargetIds() {
        Fetcher<Book> fetcher = BookFetcher.$.authors();
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("authors"))
                        .load(Entities.BOOKS_FOR_MANY_TO_MANY),
                ctx -> {
                    ctx.sql(
                            "select tb_2_.BOOK_ID, tb_1_.ID from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?)"
                    ).variables(learningGraphQLId3, graphQLInActionId3);
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                "[" +
                                        "--->{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"}, " +
                                        "--->{\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"}" +
                                        "]",
                                map.get(Entities.BOOKS_FOR_MANY_TO_MANY.get(0))
                        );
                    });
                }
        );
    }
}
