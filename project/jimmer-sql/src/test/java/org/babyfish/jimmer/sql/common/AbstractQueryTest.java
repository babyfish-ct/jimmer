package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AbstractQueryTest extends AbstractTest {

    private List<?> rows;

    protected <R> void executeAndExpect(
            TypedRootQuery<R> query,
            Consumer<QueryTestContext<R>> block
    ) {
        clearExecutions();
        jdbc(con -> {
            rows = query.execute(con);
        });
        block.accept(new QueryTestContext<R>());
    }

    protected class QueryTestContext<R> {

        public void sql(String sql) {
            List<Execution> executions = getExecutions();
            Assertions.assertFalse(
                    executions.isEmpty(),
                    "Not sql history"
            );
            Assertions.assertEquals(
                    sql,
                    executions.get(executions.size() - 1).getSql()
            );
        }

        public void variables(Object ...variables) {
            variables(Arrays.asList(variables));
        }

        public void variables(List<Object> variables) {
            List<Execution> executions = getExecutions();
            Assertions.assertFalse(
                    executions.isEmpty(),
                    "Not sql history"
            );
            Assertions.assertEquals(
                    variables,
                    executions.get(executions.size() - 1).getVariables()
            );
        }

        @SuppressWarnings("unchecked")
        public void rows(Consumer<List<R>> block) {
            Assertions.assertNotNull(
                    rows,
                    "rows is not recorded"
            );
            block.accept((List<R>) rows);
        }

        public void rows(R ... rows) {
            Assertions.assertEquals(
                    Arrays.asList(rows),
                    AbstractQueryTest.this.rows
            );
        }
    }
}
