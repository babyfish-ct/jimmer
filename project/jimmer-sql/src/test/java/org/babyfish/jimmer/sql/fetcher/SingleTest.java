package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SingleTest extends AbstractQueryTest {

    @Test
    public void testManyToOne() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(book.name().eq("GraphQL in Action"));
                    q.orderBy(book.edition());
                    return q.select(
                            book.fetch(
                                    BookFetcher.$.name().store(
                                            BookStoreFetcher.$.name(),
                                            it -> it.batch(1)
                                    )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME = ? " +
                                    "order by tb_1_.EDITION asc"
                    ).variables("GraphQL in Action");
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(manningId);
                    ctx.rows(
                            "[" +
                                    "{" +
                                    "--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\",\"name\":\"GraphQL in Action\"," +
                                    "--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\",\"name\":\"MANNING\"}" +
                                    "}," +
                                    "{" +
                                    "--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\",\"name\":\"GraphQL in Action\"," +
                                    "--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\",\"name\":\"MANNING\"}" +
                                    "}," +
                                    "{" +
                                    "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\",\"name\":\"GraphQL in Action\"," +
                                    "--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\",\"name\":\"MANNING\"}" +
                                    "}" +
                                    "]");
                }
        );
    }

    @Test
    public void testOneToMany() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    q.orderBy(store.name());
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$.name().books(
                                            BookFetcher.$.name().edition(),
                                            it -> it.batch(1).limit(3, 1).filter(args -> {
                                                args
                                                        .orderBy(args.getTable().name())
                                                        .orderBy(args.getTable().edition().desc());
                                            })
                                    )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "order by tb_1_.NAME asc");
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID = ? " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc limit ? offset ?"
                    ).variables(manningId, 3, 1);
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID = ? " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc limit ? offset ?"
                    ).variables(oreillyId, 3, 1);
                    ctx.rows(System.out::println);
                }
        );
    }

    @Test
    public void testManyToMany() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(book.name().eq("Learning GraphQL"));
                    q.orderBy(book.edition());
                    return q.select(
                            book.fetch(
                                    BookFetcher.$.name().authors(
                                            AuthorFetcher.$.firstName().lastName(),
                                            it -> it.batch(1).limit(1).filter(args -> {
                                                args.orderBy(args.getTable().firstName());
                                            })
                                    )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME = ? " +
                                    "order by tb_1_.EDITION asc"
                    ).variables("Learning GraphQL");
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID = ? " +
                                    "order by tb_1_.FIRST_NAME asc " +
                                    "limit ?"
                    ).variables(learningGraphQLId1, 1);
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID = ? " +
                                    "order by tb_1_.FIRST_NAME asc " +
                                    "limit ?"
                    ).variables(learningGraphQLId2, 1);
                    ctx.statement(3).sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID = ? " +
                                    "order by tb_1_.FIRST_NAME asc " +
                                    "limit ?"
                    ).variables(learningGraphQLId3, 1);
                }
        );
    }

    @Test
    public void testInverseManyToMany() {
        executeAndExpect(
                getLambdaClient().createQuery(AuthorTable.class, (q, author) -> {
                    q.where(author.id().in(Arrays.asList(borisId, sammerId)));
                    q.orderBy(author.firstName());
                    return q.select(
                            author.fetch(
                                    AuthorFetcher.$.firstName().lastName().books(
                                            BookFetcher.$.name().edition(),
                                            it -> it.batch(1).limit(2).filter(args -> {
                                                args.orderBy(args.getTable().edition().desc());
                                            })
                                    )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.FIRST_NAME asc"
                    ).variables(borisId, sammerId);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID = ? " +
                                    "order by tb_1_.EDITION " +
                                    "desc limit ?"
                    ).variables(borisId, 2);
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID = ? " +
                                    "order by tb_1_.EDITION " +
                                    "desc limit ?"
                    ).variables(sammerId, 2);
                }
        );
    }
}
