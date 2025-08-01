package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.query.MutableRecursiveBaseQuery;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
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
                        recursiveRef -> {
                            MutableRecursiveBaseQuery<BaseTable2<TreeNodeTable, NumericExpression<Integer>>> q =
                                    getSqlClient()
                                            .createBaseQuery(
                                                    table,
                                                    recursiveRef,
                                                    (t, r) -> t.parentId().eq(r.get_1().id())
                                            );
                            return q
                                    .addSelect(table)
                                    .addSelect(
                                            q.recursive().get_2().plus(Expression.constant(1))
                                    );
                        }
                ).asCteBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .orderBy(baseTable.get_2(), baseTable.get_1().name())
                        .select(
                                baseTable.get_1().fetch(TreeNodeFetcher.$.name()),
                                baseTable.get_2()
                        ),
                ctx -> {
                    ctx.sql(
                            "with tb_1_(c1, c2, c3) as (" +
                                    "--->select " +
                                    "--->--->tb_2_.NODE_ID, tb_2_.NAME, " +
                                    "--->--->1 " +
                                    "--->from TREE_NODE tb_2_ " +
                                    "--->where tb_2_.PARENT_ID is null " +
                                    "--->union all " +
                                    "--->select " +
                                    "--->--->tb_4_.NODE_ID, tb_4_.NAME, " +
                                    "--->--->tb_1_.c3 + 1 " +
                                    "--->from TREE_NODE tb_4_ " +
                                    "--->inner join tb_1_ on tb_4_.PARENT_ID = tb_1_.c1" +
                                    ") " +
                                    "select " +
                                    "--->tb_1_.c1, tb_1_.c2, " +
                                    "--->tb_1_.c3 " +
                                    "from tb_1_ " +
                                    "order by tb_1_.c3 asc, tb_1_.c2 asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"_1\":{\"id\":1,\"name\":\"Home\"}," +
                                    "--->--->\"_2\":1" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":9,\"name\":\"Clothing\"}," +
                                    "--->--->\"_2\":2" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":2,\"name\":\"Food\"}," +
                                    "--->--->\"_2\":2" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":6,\"name\":\"Bread\"}," +
                                    "--->--->\"_2\":3" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":3,\"name\":\"Drinks\"}," +
                                    "--->--->\"_2\":3" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":18,\"name\":\"Man\"}," +
                                    "--->--->\"_2\":3" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":10,\"name\":\"Woman\"}," +
                                    "--->--->\"_2\":3" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":7,\"name\":\"Baguette\"}," +
                                    "--->--->\"_2\":4" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":19,\"name\":\"Casual wear\"}," +
                                    "--->--->\"_2\":4" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":11,\"name\":\"Casual wear\"}," +
                                    "--->--->\"_2\":4" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":8,\"name\":\"Ciabatta\"}," +
                                    "--->--->\"_2\":4" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":4,\"name\":\"Coca Cola\"}," +
                                    "--->--->\"_2\":4" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":5,\"name\":\"Fanta\"}," +
                                    "--->--->\"_2\":4" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":22,\"name\":\"Formal wear\"}," +
                                    "--->--->\"_2\":4" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":15,\"name\":\"Formal wear\"}," +
                                    "--->--->\"_2\":4" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":12,\"name\":\"Dress\"}," +
                                    "--->--->\"_2\":5" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":20,\"name\":\"Jacket\"}," +
                                    "--->--->\"_2\":5" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":21,\"name\":\"Jeans\"}," +
                                    "--->--->\"_2\":5" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":14,\"name\":\"Jeans\"}," +
                                    "--->--->\"_2\":5" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":13,\"name\":\"Miniskirt\"}," +
                                    "--->--->\"_2\":5" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":24,\"name\":\"Shirt\"}," +
                                    "--->--->\"_2\":5" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":17,\"name\":\"Shirt\"}," +
                                    "--->--->\"_2\":5" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":23,\"name\":\"Suit\"}," +
                                    "--->--->\"_2\":5" +
                                    "--->},{" +
                                    "--->--->\"_1\":{\"id\":16,\"name\":\"Suit\"}," +
                                    "--->--->\"_2\":5" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
