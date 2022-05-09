package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import javax.persistence.criteria.JoinType;

public class InverseJoinTest extends AbstractQueryTest {

    @Test
    public void testReverseJoinOnInverseProp() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book
                                    .inverseJoinByTable(AuthorTable.class, "books")
                                    .firstName()
                                    .eq("Alex")
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK as tb_1 " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2 on tb_1.ID = tb_2.BOOK_ID " +
                                    "inner join AUTHOR as tb_3 on tb_2.AUTHOR_ID = tb_3.ID " +
                                    "where tb_3.FIRST_NAME = ?"
                    );
                    ctx.variables("Alex");
                }
        );
    }

    @Test
    public void testReverseJoinOnNormalProp() {
        executeAndExpect(
                AuthorTable.createQuery(getSqlClient(), (q, author) -> {
                    q.where(
                            author
                                    .inverseJoinByTable(BookTable.class, "authors")
                                    .name()
                                    .eq("Learning GraphQL")
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from AUTHOR as tb_1 " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2 on tb_1.ID = tb_2.AUTHOR_ID " +
                                    "inner join BOOK as tb_3 on tb_2.BOOK_ID = tb_3.ID " +
                                    "where tb_3.NAME = ?"
                    );
                    ctx.variables("Learning GraphQL");
                }
        );
    }

    @Test
    public void testInverseHalfJoinOnInverseProp() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book
                                    .inverseJoinByTable(AuthorTable.class, "books")
                                    .id()
                                    .in(alexId, danId)
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
                    ctx.variables(alexId, danId);
                }
        );
    }

    @Test
    public void testInverseHalfJoinOnNormalProp() {
        executeAndExpect(
                AuthorTable.createQuery(getSqlClient(), (q, author) -> {
                    q.where(
                            author
                                    .inverseJoinByTable(BookTable.class, "authors")
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
    public void mergeNormalJoinsAndInverseJoins() {
        executeAndExpect(
                BookStoreTable.createQuery(getSqlClient(), (q, store) -> {
                    q.where(
                            store
                                    .inverseJoinByTable(BookTable.class, "store", JoinType.LEFT)
                                    .inverseJoinByTable(AuthorTable.class, "books", JoinType.LEFT)
                                    .firstName()
                                    .eq("Alex").or(
                                            store
                                                    .<BookTable>join("books", JoinType.LEFT)
                                                    .<AuthorTable>join("authors", JoinType.LEFT)
                                                    .firstName()
                                                    .eq("Tim")
                                    )
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK_STORE as tb_1 " +
                                    "left join BOOK as tb_2 on tb_1.ID = tb_2.STORE_ID " +
                                    "left join BOOK_AUTHOR_MAPPING as tb_3 on tb_2.ID = tb_3.BOOK_ID " +
                                    "left join AUTHOR as tb_4 on tb_3.AUTHOR_ID = tb_4.ID " +
                                    "where tb_4.FIRST_NAME = ? or tb_4.FIRST_NAME = ?"
                    );
                    ctx.variables("Alex", "Tim");
                }
        );
    }

    @Test
    public void mergeNormalJoinsAndInverseJoinsWithDiffJoinTypes() {
        executeAndExpect(
                BookStoreTable.createQuery(getSqlClient(), (q, store) -> {
                    q.where(
                            store
                                    .inverseJoinByTable(BookTable.class, "store", JoinType.LEFT)
                                    .inverseJoinByTable(AuthorTable.class, "books", JoinType.LEFT)
                                    .firstName()
                                    .eq("Alex").or(
                                            store
                                                    .<BookTable>join("books", JoinType.RIGHT)
                                                    .<AuthorTable>join("authors", JoinType.RIGHT)
                                                    .firstName()
                                                    .eq("Tim")
                                    )
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
                                    "where tb_4.FIRST_NAME = ? or tb_4.FIRST_NAME = ?"
                    );
                    ctx.variables("Alex", "Tim");
                }
        );
    }
}
