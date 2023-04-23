package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.DataLoader;
import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class TransientWithCacheTest extends AbstractCachedLoaderTest {

    @Test
    public void test() {
        Fetcher<BookStore> fetcher = BookStoreFetcher.$.avgPrice();
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            connectAndExpect(
                    con -> {
                        return new DataLoader(getCachedSqlClient(), con, fetcher.getFieldMap().get("avgPrice"))
                                .load(Entities.BOOK_STORES);
                    },
                    ctx -> {
                        if (useSql) {
                            ctx.sql(
                                    "select tb_1_.ID, coalesce(avg(tb_2_.PRICE), ?) " +
                                            "from BOOK_STORE tb_1_ " +
                                            "left join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                            "where tb_1_.ID in (?, ?) " +
                                            "group by tb_1_.ID"
                            );
                        }
                        ctx.row(0, map -> {
                            Assertions.assertEquals(
                                    new BigDecimal("58.500000000000"),
                                    map.get(Entities.BOOK_STORES.get(0))
                            );
                            Assertions.assertEquals(
                                    new BigDecimal("80.333333333333"),
                                    map.get(Entities.BOOK_STORES.get(1))
                            );
                        });
                    }
            );
        }
    }
}
