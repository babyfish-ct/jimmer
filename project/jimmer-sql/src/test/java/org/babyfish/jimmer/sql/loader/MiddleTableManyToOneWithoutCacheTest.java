package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.DataLoader;
import org.babyfish.jimmer.sql.model.Author;
import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.CountryFetcher;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class MiddleTableManyToOneWithoutCacheTest extends AbstractQueryTest {

    @Test
    public void loadTargetIds() {
        Fetcher<Author> fetcher = AuthorFetcher.$.country();
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("country"))
                        .load(Entities.AUTHORS_FOR_MANY_TO_ONE),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.AUTHOR_ID, tb_1_.COUNTRY_CODE " +
                                    "from AUTHOR_COUNTRY_MAPPING tb_1_ " +
                                    "where tb_1_.AUTHOR_ID in (?, ?)"
                    ).variables(alexId, danId);
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                "{\"code\":\"USA\"}",
                                map.get(Entities.AUTHORS_FOR_MANY_TO_ONE.get(0))
                        );
                        expect(
                                "{\"code\":\"USA\"}",
                                map.get(Entities.AUTHORS_FOR_MANY_TO_ONE.get(1))
                        );
                    });
                }
        );
    }

    @Test
    public void loadTargetIdsWithFilter() {
        Fetcher<Author> fetcher = AuthorFetcher.$.country(
                CountryFetcher.$,
                it -> it.filter(
                        args -> args.where(args.getTable().code().eq("UK"))
                )
        );
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("country"))
                        .load(Entities.AUTHORS_FOR_MANY_TO_ONE),
                ctx -> {
                    ctx.sql(
                            "select tb_2_.AUTHOR_ID, tb_1_.CODE " +
                                    "from COUNTRY tb_1_ " +
                                    "inner join AUTHOR_COUNTRY_MAPPING tb_2_ on tb_1_.CODE = tb_2_.COUNTRY_CODE " +
                                    "where tb_2_.AUTHOR_ID in (?, ?) " +
                                    "and tb_1_.CODE = ?"
                    ).variables(alexId, danId, "UK");
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                null,
                                map.get(Entities.AUTHORS_FOR_MANY_TO_ONE.get(0))
                        );
                        expect(
                                null,
                                map.get(Entities.AUTHORS_FOR_MANY_TO_ONE.get(1))
                        );
                    });
                }
        );
    }

    @Test
    public void loadTargetDetails() {
        Fetcher<Author> fetcher = AuthorFetcher.$.country(
                CountryFetcher.$.allScalarFields()
        );
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("country"))
                        .load(Entities.AUTHORS_FOR_MANY_TO_ONE),
                ctx -> {
                    ctx.sql(
                            "select tb_2_.AUTHOR_ID, tb_1_.CODE, tb_1_.NAME " +
                                    "from COUNTRY tb_1_ " +
                                    "inner join AUTHOR_COUNTRY_MAPPING tb_2_ on tb_1_.CODE = tb_2_.COUNTRY_CODE " +
                                    "where tb_2_.AUTHOR_ID in (?, ?)"
                    ).variables(alexId, danId);
                    ctx.rows(1);
                    ctx.row(0, map -> {
                        expect(
                                "{\"code\":\"USA\",\"name\":\"The United States of America\"}",
                                map.get(Entities.AUTHORS_FOR_MANY_TO_ONE.get(0))
                        );
                        expect(
                                "{\"code\":\"USA\",\"name\":\"The United States of America\"}",
                                map.get(Entities.AUTHORS_FOR_MANY_TO_ONE.get(1))
                        );
                    });
                }
        );
    }
}
