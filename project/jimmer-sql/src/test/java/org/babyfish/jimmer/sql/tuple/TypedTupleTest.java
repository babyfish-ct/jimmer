package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.TypedTuple;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiFunction;

public class TypedTupleTest {

    @Test
    public void testForm() {
        BookTable book = BookTable.$;
        TypedBaseQuery<BookTupleTable> baseQuery = createBaseQuery(book)
                .select(
                        BookTupleTable
                                .construct()
                                .book(book)
                                .rank(
                                        Expression.numeric().sql(
                                                Integer.class,
                                                "rank() over(partition by %e order by %e desc)",
                                                new Expression<?>[] {book.storeId(), book.price()}
                                        )
                                )
                );
        BookTupleTable table = BookTupleTable.$;
        List<BookTuple> tuples = createQuery(baseQuery)
                .where(table.rank().between(20, 30))
                .select(
                        table.fetchBook(
                                BookFetcher.$
                                        .allScalarFields()
                        )
                )
                .execute();
    }

    @Test
    public void testCte() {
        TreeNodeTable treeNode = TreeNodeTable.$;
        TreeNodeCTETable table = TreeNodeCTETable.$;
        TypedBaseQuery<TreeNodeCTETable> baseQuery =
                createBaseQuery(treeNode)
                        .select(
                                TreeNodeCTETable
                                        .construct()
                                        .treeNode(treeNode)
                                        .depth(Expression.constant(1))
                        )
                        .unionAll(
                                createBaseQuery(treeNode)
                                        .where(treeNode.parentId().eq(table.treeNode().id()))
                                        .select(
                                                TreeNodeCTETable
                                                        .construct()
                                                        .treeNode(treeNode)
                                                        .depth(table.depth().plus(1))
                                        )
                        );
        List<TreeNodeCTE> tuples = createQuery(baseQuery)
                .where(table.treeNode().parentId().isNotNull())
                .select(table)
                .execute();
    }

    @TypedTuple
    interface BookTuple {
        Book book();
        int rank();
    }

    @TypedTuple
    interface TreeNodeCTE {
        TreeNode treeNode();
        int depth();
    }


    static native <T extends TableProxy<?>> MutableBaseQuery<T> createBaseQuery(T table);

    static native <T extends TupleTable<?>, R> MutableRootQuery<T> createQuery(
            TypedBaseQuery<T> baseQuery
    );

    interface TupleTable<T> extends Table<T> {}

    static abstract class BookTupleTable implements TupleTable<BookTuple> {

        static BookTupleTable $ = instance();
        static BookTupleTable instance() { throw new UnsupportedOperationException(); };

        BookTable book() {
            throw new UnsupportedOperationException();
        }
        NumericExpression<Integer> rank() {
            throw new UnsupportedOperationException();
        }
        static BookConstructor construct() {
            return new BookConstructor();
        }
        static class BookConstructor {
            native RankConstructor book(BookTable book);
        }
        static class RankConstructor {
            native BaseQueryMapper<BookTupleTable> rank(NumericExpression<Integer> rank);
        }
        native BookTupleTable fetchBook(Fetcher<Book> book);
    }

    static abstract class TreeNodeCTETable implements TupleTable<TreeNodeCTE> {

        static TreeNodeCTETable $ = instance();
        static TreeNodeCTETable instance() { throw new UnsupportedOperationException(); };

        TreeNodeTable treeNode() {
            throw new UnsupportedOperationException();
        }
        NumericExpression<Integer> depth() {
            throw new UnsupportedOperationException();
        }
        static TreeNodeConstructor construct() {
            return new TreeNodeConstructor();
        }
        static class TreeNodeConstructor {
            native DepthConstructor treeNode(TreeNodeTable treeNode);
        }
        static class DepthConstructor {
            native BaseQueryMapper<TreeNodeCTETable> depth(NumericExpression<Integer> depth);
        }
        native BookTupleTable fetchBook(Fetcher<Book> book);
    }

    interface MutableBaseQuery<T extends Table<?>> extends MutableQuery {
        MutableBaseQuery<T> where(Predicate... predicates);
        <R extends TupleTable<?>> TypedBaseQuery<R> select(BaseQueryMapper<R> mapper);
    }

    interface TypedBaseQuery<R> {
        TypedBaseQuery<R> unionAll(TypedBaseQuery<R> other);
    }

    interface BaseQueryMapper<R extends TupleTable<?>> {

    }

    interface BaseQueryMapping<R extends TupleTable<?>> {

    }
}
