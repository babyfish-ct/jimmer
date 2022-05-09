package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import javax.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.Arrays;

public class JoinTest extends AbstractQueryTest {

    @Test
    public void testSimple() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1"
                    );
                }
        );
    }

    @Test
    public void testMergedJoinFromParentToChild() {
        executeAndExpect(
                BookStoreTable.createQuery(getSqlClient(), (q, store) -> {
                    q.where(
                            store.<BookTable>join("books", JoinType.LEFT).price()
                                    .ge(new BigDecimal(20))
                    );
                    q.where(
                            store.<BookTable>join("books").price()
                                    .le(new BigDecimal(30))
                    );
                    q.where(
                            store
                                    .<BookTable>join("books")
                                    .<AuthorTable>join("authors")
                                    .firstName()
                                    .ilike("Alex")
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK_STORE as tb_1 " +
                                    "inner join BOOK as tb_2 on tb_1.ID = tb_2.STORE_ID " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_3 on tb_2.ID = tb_3.BOOK_ID " +
                                    "inner join AUTHOR as tb_4 on tb_3.AUTHOR_ID = tb_4.ID " +
                                    "where tb_2.PRICE >= ? and tb_2.PRICE <= ? and lower(tb_4.FIRST_NAME) like ?"
                    );
                    ctx.variables(
                            new BigDecimal(20), new BigDecimal(30), "%alex%"
                    );
                }
        );
    }

    @Test
    public void testMergedJoinFromChildToParent() {
        executeAndExpect(
                AuthorTable.createQuery(getSqlClient(), (q, author) -> {
                    q.where(
                            author
                                    .<BookTable>join("books", JoinType.LEFT)
                                    .price()
                                    .ge(new BigDecimal(20))
                    );
                    q.where(
                            author
                                    .<BookTable>join("books")
                                    .price()
                                    .le(new BigDecimal(30))
                    );
                    q.where(
                            author
                                    .<BookTable>join("books")
                                    .store()
                                    .name()
                                    .ilike("MANNING")
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from AUTHOR as tb_1 " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2 on tb_1.ID = tb_2.AUTHOR_ID " +
                                    "inner join BOOK as tb_3 on tb_2.BOOK_ID = tb_3.ID " +
                                    "inner join BOOK_STORE as tb_4 on tb_3.STORE_ID = tb_4.ID " +
                                    "where tb_3.PRICE >= ? and tb_3.PRICE <= ? and lower(tb_4.NAME) like ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), "%manning%");
                }
        );
    }

    @Test
    public void testUnnecessaryJoin() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book.store().id().in(oreillyId, manningId)
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 from BOOK as tb_1 where tb_1.STORE_ID in (?, ?)"
                    );
                    ctx.variables(oreillyId, manningId);
                }
        );
    }

    @Test
    public void testHalfJoin() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book.<AuthorTable>join("authors")
                                    .id()
                                    .in(alexId, borisId)
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK as tb_1 " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2 on tb_1.ID = tb_2.BOOK_ID " +
                                    "where tb_2.AUTHOR_ID in (?, ?)"
                    );
                    ctx.variables(alexId, borisId);
                }
        );
    }

    @Test
    public void testHalfInverseJoin() {
        executeAndExpect(
                AuthorTable.createQuery(getSqlClient(), (q, author) -> {
                    q.where(
                            author
                                    .<BookTable>join("books")
                                    .id()
                                    .in(learningGraphQLId1, learningGraphQLId2)
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from AUTHOR as tb_1 " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2 on tb_1.ID = tb_2.AUTHOR_ID " +
                                    "where tb_2.BOOK_ID in (?, ?)"
                    );
                    ctx.variables(learningGraphQLId1, learningGraphQLId2);
                }
        );
    }

    @Test
    public void testOneToManyCannotBeOptimized() {
        executeAndExpect(
                BookStoreTable.createQuery(getSqlClient(), (q, store) -> {
                    q.where(
                            store
                                    .<BookTable>join("books")
                                    .id()
                                    .in(learningGraphQLId1, learningGraphQLId2)
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK_STORE as tb_1 " +
                                    "inner join BOOK as tb_2 on tb_1.ID = tb_2.STORE_ID " +
                                    "where tb_2.ID in (?, ?)"
                    );
                    ctx.variables(learningGraphQLId1, learningGraphQLId2);
                }
        );
    }

    @Test
    public void testOuterJoin() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book.store(JoinType.LEFT).id().isNotNull().or(
                                    book.store(JoinType.LEFT).name().ilike("MANNING")
                            )
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK as tb_1 " +
                                    "left join BOOK_STORE as tb_2 on tb_1.STORE_ID = tb_2.ID " +
                                    "where tb_1.STORE_ID is not null " +
                                    "or lower(tb_2.NAME) like ?"
                    );
                    ctx.variables("%manning%");
                }
        );
    }
}
