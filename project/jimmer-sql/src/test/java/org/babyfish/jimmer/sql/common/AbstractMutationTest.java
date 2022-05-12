package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.sql.ast.Executable;
import org.junit.jupiter.api.Assertions;

import javax.sql.DataSource;
import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractMutationTest extends AbstractTest {

    protected void executeAndExpectRowCount(
            Executable<Integer> executable,
            Consumer<ExpectDSLWithRowCount> block
    ) {
        executeAndExpectRowCount(null, executable, block);
    }

    protected void executeAndExpectRowCount(
            DataSource dataSource,
            Executable<Integer> executable,
            Consumer<ExpectDSLWithRowCount> block
    ) {
        jdbc(dataSource, true, con -> {
            clearExecutions();
            int affectedRowCount = 0;
            Throwable throwable = null;
            try {
                affectedRowCount = executable.execute(con);
            } catch (Throwable ex) {
                throwable = ex;
            }
            assertRowCount(throwable, affectedRowCount, block);
        });
    }

    protected void executeAndExpectRowCountMap(
            Executable<Map<String, Integer>> executable,
            Consumer<ExpectDSLWithRowCountMap> block
    ) {
        jdbc(null, true, con -> {
            clearExecutions();
            Map<String, Integer> affectedRowCountMap;
            Throwable throwable = null;
            try {
                affectedRowCountMap = executable.execute(con);
            } catch (Throwable ex) {
                throwable = ex;
                affectedRowCountMap = Collections.emptyMap();
            }
            assertRowCountMap(throwable, affectedRowCountMap, block);
        });
    }

    private void assertRowCount(
        Throwable throwable,
        int rowCount,
        Consumer<ExpectDSLWithRowCount> block
    ) {
        ExpectDSLWithRowCount dsl = new ExpectDSLWithRowCount(getExecutions(), throwable, rowCount);
        block.accept(dsl);
        dsl.close();
    }

    private void assertRowCountMap(
            Throwable throwable,
            Map<String, Integer> rowCountMap,
            Consumer<ExpectDSLWithRowCountMap> block
    ) {
        ExpectDSLWithRowCountMap dsl = new ExpectDSLWithRowCountMap(getExecutions(), throwable, rowCountMap);
        block.accept(dsl);
        dsl.close();
    }

    protected static class ExpectDSL {

        private List<Execution> executions;

        protected Throwable throwable;

        private int statementCount = 0;

        private boolean throwableChecked = false;

        public ExpectDSL(List<Execution> executions, Throwable throwable) {
            this.executions = executions;
            this.throwable = throwable;
        }

        public void statement(Consumer<StatementDSL> block) {
            int index = statementCount++;
            if (index < executions.size()) {
                block.accept(new StatementDSL(index, executions.get(index)));
            } else if (throwable != null) {
                rethrow(throwable);
            } else {
                Assertions.fail("Two many statements, max statement count: " + executions.size());
            }
        }

        public void throwable(Consumer<ThrowableDSL> block) {
            Assertions.assertNotNull(throwable, "No throwable.");
            block.accept(new ThrowableDSL(throwable));
            throwableChecked = true;
        }

        public void close() {
            Assertions.assertEquals(
                    statementCount,
                    executions.size(),
                    "Error statement count."
            );
            if (throwable != null) {
                if (!throwableChecked) {
                    rethrow(throwable);
                }
            }
        }
    }


    protected static class ExpectDSLWithRowCount extends ExpectDSL {

        private int rowCount;

        public ExpectDSLWithRowCount(List<Execution> executions, Throwable throwable, int rowCount) {
            super(executions, throwable);
            this.rowCount = rowCount;
        }

        public void rowCount(int rowCount) {
            if (throwable == null) {
                Assertions.assertEquals(rowCount, this.rowCount, "bad row count");
            }
        }
    }

    protected static class ExpectDSLWithRowCountMap extends ExpectDSL {

        private Map<String, Integer> rowCountMap;

        public ExpectDSLWithRowCountMap(
                List<Execution> executions,
                Throwable throwable,
                Map<String, Integer> rowCountMap
        ) {
            super(executions, throwable);
            this.rowCountMap = rowCountMap;
        }

        public ExpectDSLWithRowCountMap totalRowCount(int totalRowCount) {
            int actualTotalCount = 0;
            for (Integer c : rowCountMap.values()) {
                actualTotalCount += c;
            }
            Assertions.assertEquals(totalRowCount, actualTotalCount);
            return this;
        }

        public ExpectDSLWithRowCountMap rowCount(String tableName, int rowCount) {
            Integer actualRowCount = rowCountMap.get(tableName);
            Assertions.assertEquals(
                    rowCount,
                    actualRowCount != null ? actualRowCount : 0,
                    "rowCountMap['" + tableName + "']"
            );
            return this;
        }
    }

    protected static class StatementDSL {

        private int index;

        private Execution execution;

        StatementDSL(int index, Execution execution) {
            this.index = index;
            this.execution = execution;
        }

        public void sql(String value) {
            Assertions.assertEquals(
                    value, execution.getSql(),
                    "statements[" + index + "].sql"
            );
        }

        public void variables(Object ... values) {
            Assertions.assertEquals(
                    values.length,
                    execution.getVariables().size(),
                    "statements[" + index + "].variables.size."
            );
            for (int i = 0; i < values.length; i++) {
                Object exp = values[i];
                Object act = execution.getVariables().get(i);
                if (exp instanceof byte[]) {
                    Assertions.assertTrue(
                            Arrays.equals((byte[])exp, (byte[])act),
                            "statements[" + index + "].variables[$" + i + "]."
                    );
                } else {
                    Assertions.assertEquals(
                            exp,
                            act,
                            "statements[" + index + "].variables[$" + i + "]."
                    );
                }
            }
        }

        public void unorderedVariables(Object ... values) {
            Assertions.assertEquals(
                    new HashSet<Object>(Arrays.asList(values)),
                    new HashSet<>(execution.getVariables()),
                    "statements[" + index + "].variables."
            );
        }

        public void variables(Consumer<List<Object>> block) {
            block.accept(execution.getVariables());
        }
    }

    protected static class ThrowableDSL {

        private Throwable throwable;

        ThrowableDSL(Throwable throwable) {
            this.throwable = throwable;
        }

        public void type(Class<? extends Throwable> type) {
            Assertions.assertSame(type, throwable.getClass());
        }

        public void message(String message) {
            Assertions.assertEquals(message, throwable.getMessage());
        }

        public void detail(Consumer<Throwable> block) {
            block.accept(throwable);
        }
    }

    private static void rethrow(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        throw new RuntimeException(throwable);
    }
}
