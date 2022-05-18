package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
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
        rows = null;
        jdbc(con -> {
            if (rows == null) {
                rows = query.execute(con);
            }
        });
        block.accept(new QueryTestContext<>(0));
    }

    protected class QueryTestContext<R> {

        private int index;

        QueryTestContext(int index) {
            this.index = index;
        }

        public QueryTestContext<R> statement(int index) {
            return new QueryTestContext<>(index);
        }

        public void sql(String sql) {
            List<Execution> executions = getExecutions();
            Assertions.assertFalse(
                    executions.isEmpty(),
                    "Not sql history"
            );
            Assertions.assertEquals(
                    sql,
                    executions.get(index).getSql(),
                    "statements[" + index + "].sql"
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
                    executions.get(index).getVariables(),
                    "statements[" + index + "].variables"
            );
        }

        @SuppressWarnings("unchecked")
        public void rows(Consumer<List<R>> block) {
            block.accept((List<R>) rows);
        }

        public void rows(List<R> rows) {
            Assertions.assertEquals(
                    rows,
                    AbstractQueryTest.this.rows
            );
        }
    }
}
