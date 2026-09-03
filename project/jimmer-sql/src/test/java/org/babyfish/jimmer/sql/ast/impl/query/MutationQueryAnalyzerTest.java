package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.tuple.BookUpdateReturningTupleMapper;
import org.babyfish.jimmer.sql.tuple.BookUpdateReturningTupleTable;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.graphQLInActionId1;
import static org.babyfish.jimmer.sql.common.Constants.learningGraphQLId1;

public class MutationQueryAnalyzerTest extends AbstractMutationTest {

    @Test
    public void testGlobalFiltersAreAppliedToUnfrozenUnionBranches() {
        JSqlClient sqlClient = getSqlClient(it -> it.addFilters(new Filter<BookProps>() {
            @Override
            public void filter(FilterArgs<BookProps> args) {
                args.where(args.getTable().name().ne("GraphQL in Action"));
            }
        }));
        BookTable book = BookTable.$;
        ConfigurableBaseQuery<BookUpdateReturningTupleTable> firstQuery = sqlClient
                .createBaseQuery(book)
                .where(book.id().eq(learningGraphQLId1))
                .where(book.name().ne("GraphQL in Action"))
                .select(BookUpdateReturningTupleMapper.id(book.id()).name(book.name()));
        ConfigurableBaseQueryImpl<?> first = (ConfigurableBaseQueryImpl<?>) firstQuery;
        first.getMutableQuery().freeze(new AstContext((JSqlClientImplementor) sqlClient));

        BookUpdateReturningTupleTable source = TypedBaseQuery.unionAll(
                firstQuery,
                sqlClient
                        .createBaseQuery(book)
                        .where(book.id().eq(graphQLInActionId1))
                        .select(BookUpdateReturningTupleMapper.id(book.id()).name(book.name()))
        ).asBaseTable();
        BookStoreTable store = BookStoreTable.$;
        executeAndExpectRowCount(
                sqlClient
                        .createInsert(store, source)
                        .set(store.id(), source.getId())
                        .set(store.name(), source.getName()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_STORE(ID, NAME, VERSION) " +
                                        "select tb_1_.c1, tb_1_.c2, ? from (" +
                                        "select tb_1_.ID c1, tb_1_.NAME c2 from BOOK tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.NAME <> ? " +
                                        "union all " +
                                        "select tb_2_.ID c1, tb_2_.NAME c2 from BOOK tb_2_ " +
                                        "where tb_2_.ID = ? and tb_2_.NAME <> ?" +
                                        ") tb_1_"
                        );
                        it.variables(
                                0,
                                learningGraphQLId1,
                                "GraphQL in Action",
                                graphQLInActionId1,
                                "GraphQL in Action"
                        );
                    });
                    ctx.rowCount(1);
                }
        );
    }
}
