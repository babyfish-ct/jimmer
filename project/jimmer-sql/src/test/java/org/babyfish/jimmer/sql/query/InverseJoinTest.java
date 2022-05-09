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
                BookTable.createQuery(getSqlClient(), (query, book) ->
                        query
                                .where(
                                        book
                                                .inverseJoinByTable(AuthorTable.class, "books")
                                                .firstName()
                                                .eq("Alex")
                                )
                                .select(Expression.constant(1))
                ),
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
                AuthorTable.createQuery(getSqlClient(), (query, author) ->
                        query
                                .where(
                                        author
                                                .inverseJoinByTable(BookTable.class, "authors")
                                                .name()
                                                .eq("Learning GraphQL")
                                )
                                .select(Expression.constant(1))
                ),
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
                BookTable.createQuery(getSqlClient(), (query, book) ->
                        query
                                .where(
                                        book
                                                .inverseJoinByTable(AuthorTable.class, "books")
                                                .id()
                                                .in(alexId, danId)
                                )
                                .select(Expression.constant(1))
                ),
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
                AuthorTable.createQuery(getSqlClient(), (query, author) ->
                        query
                                .where(
                                        author
                                                .inverseJoinByTable(BookTable.class, "authors")
                                                .id()
                                                .in(learningGraphQLId1, learningGraphQLId2)
                                )
                                .select(Expression.constant(1))
                ),
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
                BookStoreTable.createQuery(getSqlClient(), (query, store) ->
                        query
                                .where(
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
                                )
                                .select(Expression.constant(1))
                ),
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
                BookStoreTable.createQuery(getSqlClient(), (query, store) ->
                        query
                                .where(
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
                                )
                                .select(Expression.constant(1))
                ),
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

    /*
    @Test
    fun mergeNormalJoinsAndReversedJoinsWithDiffJoinTypes() {
        sqlClient.createQuery(BookStore::class) {
            where {
                or(
                    table
                        .`←joinReference?`(Book::store)
                        .`←joinList?`(Author::books)
                        .get(Author::firstName) eq "Alex",
                    table.books.authors.firstName eq "Tim"
                )
            }
            select(constant(1))
        }.executeAndExpect {
            sql {
                """select 1 from BOOK_STORE as tb_1_
                    |inner join BOOK as tb_2_ on tb_1_.ID = tb_2_.STORE_ID
                    |inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.BOOK_ID
                    |inner join AUTHOR as tb_4_ on tb_3_.AUTHOR_ID = tb_4_.ID
                    |where tb_4_.FIRST_NAME = $1 or tb_4_.FIRST_NAME = $2""".trimMargin()
            }
            variables("Alex", "Tim")
        }
    }
     */
}
