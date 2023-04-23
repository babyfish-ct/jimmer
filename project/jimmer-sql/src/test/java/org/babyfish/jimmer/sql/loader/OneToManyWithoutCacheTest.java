package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.DataLoader;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class OneToManyWithoutCacheTest extends AbstractQueryTest {

    @Test
    public void loadChildIds() {
        Fetcher<BookStore> fetcher = BookStoreFetcher.$.books();
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("books"))
                        .load(Entities.BOOK_STORES),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.STORE_ID, tb_1_.ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?)"
                    ).variables(oreillyId, manningId);
                    ctx.rows(1);
                    ctx.row(
                            0,
                            map -> {
                                expect(
                                        "[" +
                                                "--->{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}, " +
                                                "--->{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}, " +
                                                "--->{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}, " +
                                                "--->{\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"}, " +
                                                "--->{\"id\":\"8e169cfb-2373-4e44-8cce-1f1277f730d1\"}, " +
                                                "--->{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"}, " +
                                                "--->{\"id\":\"914c8595-35cb-4f67-bbc7-8029e9e6245a\"}, " +
                                                "--->{\"id\":\"058ecfd0-047b-4979-a7dc-46ee24d08f08\"}, " +
                                                "--->{\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"}" +
                                                "]",
                                        map.get(Entities.BOOK_STORES.get(0))
                                );
                                expect(
                                        "[" +
                                                "--->{\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"}, " +
                                                "--->{\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"}, " +
                                                "--->{\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"}" +
                                                "]",
                                        map.get(Entities.BOOK_STORES.get(1))
                                );
                            }
                    );
                }
        );
    }

    @Test
    public void loadChildIdsWithFilter() {
        Fetcher<BookStore> fetcher = BookStoreFetcher.$.books(
                BookFetcher.$,
                it -> it.filter(args ->
                        args.where(args.getTable().edition().eq(3))
                )
        );
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("books"))
                        .load(Entities.BOOK_STORES),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.STORE_ID, tb_1_.ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?) " +
                                    "and tb_1_.EDITION = ?"
                    ).variables(oreillyId, manningId, 3);
                    ctx.rows(1);
                    ctx.row(
                            0,
                            map -> {
                                expect(
                                        "[" +
                                                "--->{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}, " +
                                                "--->{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"}, " +
                                                "--->{\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"}" +
                                                "]",
                                        map.get(Entities.BOOK_STORES.get(0))
                                );
                                expect(
                                        "[" +
                                                "--->{\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"}" +
                                                "]",
                                        map.get(Entities.BOOK_STORES.get(1))
                                );
                            }
                    );
                }
        );
    }

    @Test
    public void loadChildDetails() {
        Fetcher<BookStore> fetcher = BookStoreFetcher.$.books(
                BookFetcher.$.name().edition()
        );
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("books"))
                        .load(Entities.BOOK_STORES),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.STORE_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?)"
                    ).variables(oreillyId, manningId);
                    ctx.rows(1);
                    ctx.row(
                            0,
                            map -> {
                                expect(
                                        "[" +
                                                "--->{" +
                                                "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                                "--->--->\"name\":\"Learning GraphQL\"," +
                                                "--->--->\"edition\":1" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                                "--->--->\"name\":\"Learning GraphQL\"," +
                                                "--->--->\"edition\":2" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                                "--->--->\"name\":\"Learning GraphQL\"," +
                                                "--->--->\"edition\":3" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                                                "--->--->\"name\":\"Effective TypeScript\"," +
                                                "--->--->\"edition\":1" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"8e169cfb-2373-4e44-8cce-1f1277f730d1\"," +
                                                "--->--->\"name\":\"Effective TypeScript\"," +
                                                "--->--->\"edition\":2" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                                "--->--->\"name\":\"Effective TypeScript\"," +
                                                "--->--->\"edition\":3" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"914c8595-35cb-4f67-bbc7-8029e9e6245a\"," +
                                                "--->--->\"name\":\"Programming TypeScript\"," +
                                                "--->--->\"edition\":1" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"058ecfd0-047b-4979-a7dc-46ee24d08f08\"," +
                                                "--->--->\"name\":\"Programming TypeScript\"," +
                                                "--->--->\"edition\":2" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                                "--->--->\"name\":\"Programming TypeScript\"," +
                                                "--->--->\"edition\":3" +
                                                "--->}" +
                                                "]",
                                        map.get(Entities.BOOK_STORES.get(0))
                                );
                                expect(
                                        "[" +
                                                "--->{" +
                                                "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                                "--->--->\"name\":\"GraphQL in Action\"," +
                                                "--->--->\"edition\":1" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                                "--->--->\"name\":\"GraphQL in Action\"," +
                                                "--->--->\"edition\":2" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                                "--->--->\"name\":\"GraphQL in Action\"," +
                                                "--->--->\"edition\":3" +
                                                "--->}" +
                                                "]",
                                        map.get(Entities.BOOK_STORES.get(1))
                                );
                            }
                    );
                }
        );
    }

    @Test
    public void loadChildDetailsWithFilter() {
        Fetcher<BookStore> fetcher = BookStoreFetcher.$.books(
                BookFetcher.$.name().edition(),
                it -> it.filter(args ->
                        args.where(args.getTable().edition().eq(3))
                )
        );
        connectAndExpect(
                con -> new DataLoader(getSqlClient(), con, fetcher.getFieldMap().get("books"))
                        .load(Entities.BOOK_STORES),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.STORE_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?) " +
                                    "and tb_1_.EDITION = ?"
                    ).variables(oreillyId, manningId, 3);
                    ctx.rows(1);
                    ctx.row(
                            0,
                            map -> {
                                expect(
                                        "[" +
                                                "--->{" +
                                                "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                                "--->--->\"name\":\"Learning GraphQL\"," +
                                                "--->--->\"edition\":3" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                                "--->--->\"name\":\"Effective TypeScript\"," +
                                                "--->--->\"edition\":3" +
                                                "--->}, {" +
                                                "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                                "--->--->\"name\":\"Programming TypeScript\"," +
                                                "--->--->\"edition\":3" +
                                                "--->}" +
                                                "]",
                                        map.get(Entities.BOOK_STORES.get(0))
                                );
                                expect(
                                        "[" +
                                                "--->{" +
                                                "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                                "--->--->\"name\":\"GraphQL in Action\"," +
                                                "--->--->\"edition\":3" +
                                                "--->}" +
                                                "]",
                                        map.get(Entities.BOOK_STORES.get(1))
                                );
                            }
                    );
                }
        );
    }
}
