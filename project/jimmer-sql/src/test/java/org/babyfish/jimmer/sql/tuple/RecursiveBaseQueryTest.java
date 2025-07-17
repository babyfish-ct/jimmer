package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.impl.recursive.AbstractRecursiveRef;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.ast.table.base.recursive.RecursiveRef2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
import org.junit.jupiter.api.Test;

public class RecursiveBaseQueryTest extends AbstractQueryTest {

    @Test
    public void testRecursive() {
        TreeNodeTable table = TreeNodeTable.$;
        BaseTable2<TreeNodeTable, NumericExpression<Integer>> baseTable =
                TypedBaseQuery.unionAll(
                        getSqlClient()
                                .createBaseQuery(table)
                                .addSelect(table)
                                .addSelect(Expression.constant(1))
                ).asBaseTable();
    }
}
