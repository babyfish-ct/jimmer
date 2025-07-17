package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.impl.recursive.AbstractRecursiveRef;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.ast.table.base.recursive.RecursiveRef2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
import org.junit.jupiter.api.Test;

public class RecursiveBaseQueryTest extends AbstractQueryTest {

    @Test
    public void testRecursive() {
        TreeNodeTable table = TreeNodeTable.$;
        BaseTable2<TreeNodeTable, NumericExpression<Integer>> baseTable =
                TypedBaseQuery.unionAllRecursively(
                        getSqlClient()
                                .createBaseQuery(table)
                                .where(table.parentId().isNull())
                                .addSelect(table)
                                .addSelect(Expression.constant(1)),
                        recursive ->
                                getSqlClient()
                                        .createBaseQuery(table)
                                        .where(
                                                table.asTableEx().weakJoin(recursive, (t, r) ->
                                                        t.parentId().eq(r.get_1().id())
                                                ).get_1().id().isNotNull()
                                        )
                                        .addSelect(table)
                                        .addSelect(
                                                table.asTableEx().weakJoin(recursive, (t, r) ->
                                                        t.parentId().eq(r.get_1().id())
                                                ).get_2().plus(Expression.constant(1))
                                        )
                ).asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .select(
                                baseTable.get_1().fetch(TreeNodeFetcher.$.name()),
                                baseTable.get_2()
                        ),
                ctx -> {
                    ctx.sql("");
                }
        );
    }
}
