package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

public class FindTest extends AbstractQueryTest {

    @Test
    public void testFind() {
        connectAndExpect(con -> {
            return getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .findAll(
                            BookStore.class,
                            BookStoreProps.NAME.desc()
                    );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                            "from BOOK_STORE as tb_1_ " +
                            "order by tb_1_.NAME desc"
            );
            it.rows(
                    "[" +
                            "{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\",\"name\":\"O'REILLY\",\"website\":null,\"version\":0}," +
                            "{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\",\"name\":\"MANNING\",\"website\":null,\"version\":0}" +
                            "]"
            );
        });
    }

    @Test
    public void testFindByFetcher() {
        connectAndExpect(con -> {
            return getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .findAll(
                            BookStoreFetcher.$
                                    .allScalarFields()
                                    .books(
                                            BookFetcher.$.allScalarFields(),
                                            it -> it.filter(args ->
                                                    args.where(args.getTable().edition().eq(3))
                                            )
                                    ),
                            BookStoreProps.NAME.desc()
                    );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                            "from BOOK_STORE as tb_1_ " +
                            "order by tb_1_.NAME desc"
            );
            it.statement(1).sql(
                    "select tb_1_.STORE_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.STORE_ID in (?, ?) and tb_1_.EDITION = ?"
            );
            it.rows(
                    "[{" +
                            "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                            "--->\"name\":\"O'REILLY\"," +
                            "--->\"website\":null," +
                            "--->\"version\":0," +
                            "--->\"books\":[" +
                            "--->--->{" +
                            "--->--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                            "--->--->--->\"name\":\"Learning GraphQL\"," +
                            "--->--->--->\"edition\":3," +
                            "--->--->--->\"price\":51.00" +
                            "--->--->},{" +
                            "--->--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                            "--->--->--->\"name\":\"Effective TypeScript\"," +
                            "--->--->--->\"edition\":3," +
                            "--->--->--->\"price\":88.00" +
                            "--->--->},{" +
                            "--->--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                            "--->--->--->\"name\":\"Programming TypeScript\"," +
                            "--->--->--->\"edition\":3," +
                            "--->--->--->\"price\":48.00" +
                            "--->--->}" +
                            "--->]" +
                            "},{" +
                            "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                            "--->\"name\":\"MANNING\"," +
                            "--->\"website\":null," +
                            "--->\"version\":0," +
                            "--->\"books\":[" +
                            "--->--->{" +
                            "--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                            "--->--->--->\"name\":\"GraphQL in Action\"," +
                            "--->--->--->\"edition\":3," +
                            "--->--->--->\"price\":80.00" +
                            "--->--->}" +
                            "--->]" +
                            "}" +
                            "]"
            );
        });
    }

    @Test
    public void testFindByExample() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL").setEdition(3);
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con)
                    .findByExample(
                            Example.of(book).like(BookProps.NAME),
                            BookProps.NAME
                    );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.NAME like ? and tb_1_.EDITION = ? " +
                            "order by tb_1_.NAME asc"
            );
            it.rows(
                    "[{" +
                            "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                            "--->\"name\":\"GraphQL in Action\"," +
                            "--->\"edition\":3," +
                            "--->\"price\":80.00,\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                            "},{" +
                            "--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                            "--->\"name\":\"Learning GraphQL\"," +
                            "--->\"edition\":3," +
                            "--->\"price\":51.00," +
                            "--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                            "}]"
            );
        });
    }

    @Test
    public void testFindByExampleAndFetcher() {
        Book book = BookDraft.$.produce(draft -> {
            draft.applyStore(store -> store.setId(Constants.manningId));
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con)
                    .findByExample(
                            Example.of(book),
                            BookFetcher.$
                                    .allScalarFields()
                                    .store(
                                            BookStoreFetcher.$.allScalarFields()
                                    ),
                            BookProps.NAME.desc()
                    );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where tb_1_.STORE_ID = ? order by tb_1_.NAME desc"
            ).variables(Constants.manningId);
            it.statement(1).sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                            "from BOOK_STORE as tb_1_ " +
                            "where tb_1_.ID = ?"
            );
            it.rows(
                    "[" +
                            "--->{" +
                            "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                            "--->--->\"name\":\"GraphQL in Action\"," +
                            "--->--->\"edition\":1," +
                            "--->--->\"price\":80.00," +
                            "--->--->\"store\":{" +
                            "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                            "--->--->--->\"name\":\"MANNING\"," +
                            "--->--->--->\"website\":null," +
                            "--->--->--->\"version\":0" +
                            "--->--->}" +
                            "--->},{" +
                            "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                            "--->--->\"name\":\"GraphQL in Action\"," +
                            "--->--->\"edition\":2," +
                            "--->--->\"price\":81.00," +
                            "--->--->\"store\":{" +
                            "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                            "--->--->--->\"name\":\"MANNING\"," +
                            "--->--->--->\"website\":null," +
                            "--->--->--->\"version\":0" +
                            "--->--->}" +
                            "--->},{" +
                            "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                            "--->--->\"name\":\"GraphQL in Action\"," +
                            "--->--->\"edition\":3," +
                            "--->--->\"price\":80.00," +
                            "--->--->\"store\":{" +
                            "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                            "--->--->--->\"name\":\"MANNING\"," +
                            "--->--->--->\"website\":null," +
                            "--->--->--->\"version\":0" +
                            "--->--->}" +
                            "--->}" +
                            "]"
            );
        });
    }
    
    @Test
    public void testFindByExampleWithNull() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setStore((BookStore) null);
            draft.setName("X");
        });
        connectAndExpect(con -> {
            return getSqlClient().getEntities().forConnection(con).findByExample(
                    Example.of(book).ilike(BookProps.NAME, LikeMode.END)
            );
        }, it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK as tb_1_ " +
                            "where lower(tb_1_.NAME) like ? and tb_1_.STORE_ID is null"
            ).variables("%x");
        });
    }

    @Test
    public void findBySqlCalculation() {
        AuthorTable table = AuthorTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.fullName2().eq("Alex Banks"))
                        .select(
                                table.fetch(
                                        AuthorFetcher.$.fullName2()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, concat(tb_1_.FIRST_NAME, ' ', tb_1_.LAST_NAME) " +
                                    "from AUTHOR as tb_1_ " +
                                    "where concat(tb_1_.FIRST_NAME, ' ', tb_1_.LAST_NAME) = ?"
                    );
                    ctx.rows("[{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\",\"fullName2\":\"Alex Banks\"}]");
                }
        );
    }
}
