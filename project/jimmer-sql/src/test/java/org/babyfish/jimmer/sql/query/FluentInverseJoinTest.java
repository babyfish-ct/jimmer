package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class FluentInverseJoinTest extends AbstractQueryTest {

    @Test
    public void testReverseJoinOnInverseProp() {
        BookTable book = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(book)
                        .where(
                                book
                                        .inverseJoin(AuthorTableEx.class, AuthorTableEx::books)
                                        .firstName()
                                        .eq("Alex")
                        )
                        .select(Expression.constant(1)),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "inner join AUTHOR as tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_3_.FIRST_NAME = ?"
                    );
                    ctx.variables("Alex");
                }
        );
    }

    @Test
    public void testReverseJoinOnNormalProp() {
        AuthorTable author = AuthorTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(author)
                        .where(
                                author
                                        .inverseJoin(BookTableEx.class, BookTableEx::authors)
                                        .name()
                                        .eq("Learning GraphQL")
                        )
                        .select(Expression.constant(1)),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "inner join BOOK as tb_3_ on tb_2_.BOOK_ID = tb_3_.ID " +
                                    "where tb_3_.NAME = ?"
                    );
                    ctx.variables("Learning GraphQL");
                }
        );
    }

    @Test
    public void testInverseHalfJoinOnInverseProp() {
        
        BookTable book = BookTable.$;
        
        executeAndExpect(
                getSqlClient()
                        .createQuery(book)
                        .where(
                                book
                                        .inverseJoin(AuthorTableEx.class, AuthorTableEx::books)
                                        .id()
                                        .in(Arrays.asList(alexId, danId))
                        )
                        .select(Expression.constant(1)),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID in (?, ?)"
                    );
                    ctx.variables(alexId, danId);
                }
        );
    }

    @Test
    public void testInverseHalfJoinOnNormalProp() {
        
        AuthorTable author = AuthorTable.$;
        
        executeAndExpect(
                getSqlClient()
                        .createQuery(author)
                        .where(
                                author
                                        .inverseJoin(BookTableEx.class, BookTableEx::authors)
                                        .id()
                                        .in(Arrays.asList(learningGraphQLId1, learningGraphQLId2))
                        )
                        .select(Expression.constant(1)),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?)"
                    );
                    ctx.variables(learningGraphQLId1, learningGraphQLId2);
                }
        );
    }

    @Test
    public void mergeNormalJoinsAndInverseJoins() {

        BookStoreTable store = BookStoreTable.$;

        executeAndExpect(
                getSqlClient().createQuery(store)
                        .where(
                                store
                                        .inverseJoin(BookTable.class, BookTable::store, JoinType.LEFT)
                                        .inverseJoin(AuthorTableEx.class, AuthorTableEx::books, JoinType.LEFT)
                                        .firstName()
                                        .eq("Alex").or(
                                                store
                                                        .asTableEx()
                                                        .<BookTable>join ("books", JoinType.LEFT)
                                                        .<AuthorTable>join ("authors", JoinType.LEFT)
                                                        .firstName()
                                                        .eq("Tim")
                                        )
                        )
                        .select(Expression.constant(1)),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "left join BOOK as tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "left join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.BOOK_ID " +
                                    "left join AUTHOR as tb_4_ on tb_3_.AUTHOR_ID = tb_4_.ID " +
                                    "where tb_4_.FIRST_NAME = ? or tb_4_.FIRST_NAME = ?"
                    );
                    ctx.variables("Alex", "Tim");
                }
        );
    }

    @Test
    public void mergeNormalJoinsAndInverseJoinsWithDiffJoinTypes() {

        BookStoreTable store = BookStoreTable.$;

        executeAndExpect(
                getSqlClient().createQuery(store)
                        .where(
                                store
                                        .inverseJoin(BookTable.class, BookTable::store, JoinType.LEFT)
                                        .inverseJoin(AuthorTableEx.class, AuthorTableEx::books, JoinType.LEFT)
                                        .firstName()
                                        .eq("Alex").or(
                                                store
                                                        .<BookTable>join ("books", JoinType.RIGHT)
                                                        .<AuthorTable>join ("authors", JoinType.RIGHT)
                                                        .firstName()
                                                        .eq("Tim")
                                        )
                        )
                        .select(Expression.constant(1)),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "inner join BOOK as tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.BOOK_ID " +
                                    "inner join AUTHOR as tb_4_ on tb_3_.AUTHOR_ID = tb_4_.ID " +
                                    "where tb_4_.FIRST_NAME = ? or tb_4_.FIRST_NAME = ?"
                    );
                    ctx.variables("Alex", "Tim");
                }
        );
    }
}
