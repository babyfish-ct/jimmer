package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.mutation.Rows;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.babyfish.jimmer.sql.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

public class RowsTest extends AbstractQueryTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testIssue645() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        jdbc(con -> {
            Map<Object, ImmutableSpi> rowMap = Rows.findMapByKeys(
                    new SaveContext(
                            new SaveOptionsImpl(sqlClient),
                            con,
                            ImmutableType.get(TreeNode.class)
                    ),
                    QueryReason.TRIGGER,
                    (Fetcher<ImmutableSpi>) (Fetcher<?>) TreeNodeFetcher.$.allTableFields(),
                    Arrays.asList(
                            (ImmutableSpi) TreeNodeDraft.$.produce(treeNode -> {
                                treeNode.setName("Coca Cola").setParentId(3L);
                            }),
                            (ImmutableSpi) TreeNodeDraft.$.produce(treeNode -> {
                                treeNode.setName("Fanta").setParentId(3L);
                            })
                    )
            );
            Assertions.assertEquals(
                    "{" +
                            "Tuple2(_1=Coca Cola, _2=3)=" +
                            "{\"id\":4,\"name\":\"Coca Cola\",\"parent\":{\"id\":3}}, " +
                            "Tuple2(_1=Fanta, _2=3)=" +
                            "{\"id\":5,\"name\":\"Fanta\",\"parent\":{\"id\":3}}" +
                            "}",
                    rowMap.toString()
            );
        });
    }
}
