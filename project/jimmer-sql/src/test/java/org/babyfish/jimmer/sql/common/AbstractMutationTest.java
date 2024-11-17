package org.babyfish.jimmer.sql.common;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.junit.jupiter.api.Assertions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    protected void executeAndExpectResult(
            Executable<? extends MutationResult> executable,
            Consumer<ExpectDSLWithResult> block
    ) {
        executeAndExpectResult(null, executable, block);
    }

    protected void executeAndExpectResult(
            DataSource dataSource,
            Executable<? extends MutationResult> executable,
            Consumer<ExpectDSLWithResult> block
    ) {
        jdbc(dataSource, true, con -> {
            clearExecutions();
            MutationResult result;
            Throwable throwable = null;
            try {
                result = executable.execute(con);
            } catch (Throwable ex) {
                throwable = ex;
                result = null;
            }
            assertResult(throwable, result, block);
        });
    }

    protected <T> void connectAndExpect(
            Function<Connection, T> func,
            Consumer<ExpectDSLWithValue<T>> block
    ) {
        connectAndExpect(null, func, block);
    }

    protected <T> void connectAndExpect(
            DataSource dataSource,
            Function<Connection, T> func,
            Consumer<ExpectDSLWithValue<T>> block
    ) {
        jdbc(dataSource, true, con -> {
            clearExecutions();
            T value;
            Throwable throwable = null;
            try {
                value = func.apply(con);
            } catch (Throwable ex) {
                throwable = ex;
                value = null;
            }
            assertValue(throwable, value, block);
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

    private <T> void assertValue(
            Throwable throwable,
            T value,
            Consumer<ExpectDSLWithValue<T>> block
    ) {
        ExpectDSLWithValue<T> dsl = new ExpectDSLWithValue<>(getExecutions(), throwable, value);
        block.accept(dsl);
        dsl.close();
    }

    private void assertResult(
            Throwable throwable,
            MutationResult result,
            Consumer<ExpectDSLWithResult> block
    ) {
        ExpectDSLWithResult dsl = new ExpectDSLWithResult(getExecutions(), throwable, result);
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

    protected static class ExpectDSLWithValue<T> extends ExpectDSL {

        private T value;

        public ExpectDSLWithValue(List<Execution> executions, Throwable throwable, T value) {
            super(executions, throwable);
            this.value = value;
        }

        public void value(String content) {
            if (throwable == null) {
                assertContentEquals(content, this.value.toString());
            }
        }

        public void value(Consumer<T> consumer) {
            if (throwable == null) {
                consumer.accept(value);
            }
        }
    }

    protected static class ExpectDSLWithResult extends ExpectDSL {

        private MutationResult result;

        private int entityCount;

        public ExpectDSLWithResult(
                List<Execution> executions,
                Throwable throwable,
                MutationResult result
        ) {
            super(executions, throwable);
            this.result = result;
        }

        public ExpectDSLWithResult totalRowCount(int totalRowCount) {
            Assertions.assertNotNull(result);
            Assertions.assertEquals(totalRowCount, result.getTotalAffectedRowCount());
            return this;
        }

        public ExpectDSLWithResult rowCount(AffectedTable affectTable, int rowCount) {
            Assertions.assertNotNull(result);
            Assertions.assertEquals(
                    rowCount,
                    result.getAffectedRowCount(affectTable),
                    "rowCountMap['" + affectTable + "']"
            );
            return this;
        }

        public ExpectDSLWithResult entity(Consumer<EntityDSL> block) {
            if (throwable != null) {
                rethrow(throwable);
            }
            return entity(entityCount++, block);
        }

        private ExpectDSLWithResult entity(
                int index,
                Consumer<EntityDSL> block
        ) {
            MutationResultItem<?> item;
            if (index == 0) {
                if (result instanceof SimpleSaveResult<?>) {
                    item = (SimpleSaveResult<?>) result;
                } else {
                    item = ((BatchSaveResult<?>) result).getItems().get(0);
                }
            } else {
                item = ((BatchSaveResult<?>) result).getItems().get(index);
            }
            block.accept(new EntityDSL(index, item));
            return this;
        }

        @Override
        public void close() {
            super.close();
            int actualEntityCount;
            if (result instanceof SimpleSaveResult<?>) {
                actualEntityCount = 1;
            } else if (result instanceof BatchSaveResult<?> ){
                actualEntityCount = ((BatchSaveResult<?>) result).getItems().size();
            } else {
                actualEntityCount = 0;
            }
            Assertions.assertEquals(
                    entityCount,
                    actualEntityCount,
                    "entity.count"
            );
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
                    value.replace("--->", ""),
                    execution.getSql(),
                    "statements[" + index + "].sql"
            );
        }

        public void queryReason(QueryReason queryReason) {
            Assertions.assertEquals(ExecutionPurpose.Type.COMMAND, execution.getPurpose().getType());
            Assertions.assertEquals(
                    queryReason,
                    ((ExecutionPurpose.Command)execution.getPurpose()).getQueryReason(),
                    "Illegal query reason of statements[" + this.index + "], expected: " +
                            queryReason +
                            ", actual: " +
                            ((ExecutionPurpose.Command)execution.getPurpose()).getQueryReason()
            );
        }

        public void variables(Object ... values) {
            batchVariables(0, values);
            batches(1);
        }

        public void batches(int batchCount) {
            Assertions.assertEquals(batchCount, execution.getBatchCount());
        }

        public void batchVariables(int batchIndex, Object ... values) {
            Assertions.assertEquals(
                    values.length,
                    execution.getVariables(batchIndex).size(),
                    "statements[" + index + "].batch[" + batchIndex + "].variables.size is error, actual variables: " +
                            execution.getVariables(batchIndex)
            );
            for (int i = 0; i < values.length; i++) {
                Object exp = values[i];
                if (exp == UNKNOWN_VARIABLE) {
                    continue;
                }
                Object act = execution.getVariables(batchIndex).get(i);
                if (act instanceof TypedList<?>) {
                    act = ((TypedList<?>)act).toArray();
                }
                if (exp != null && exp.getClass().isArray()) {
                    Assertions.assertTrue(
                            new EqualsBuilder().append(exp, act).isEquals(),
                            "statements[" + index + "].batch[" + batchIndex + "].variables[" + i + "] is error, " +
                                    "expected variables: " +
                                    Arrays.toString(values) +
                                    ", actual variables: " +
                                    execution.getVariables(batchIndex)
                    );
                } else {
                    Assertions.assertEquals(
                            exp,
                            act,
                            "statements[" + index + "].batch[" + batchIndex + "].variables[" + i + "] is error, " +
                                    "expected variables: " +
                                    Arrays.toString(values) +
                                    ", actual variables: " +
                                    execution.getVariables(batchIndex)
                    );
                }
            }
        }

        public void unorderedVariables(Object ... values) {
            unorderedBatchVariables(0, values);
        }

        public void unorderedBatchVariables(int batchIndex, Object ... values) {
            Assertions.assertEquals(
                    values.length,
                    this.execution.getVariables(batchIndex).size(),
                    "statements[" + index + "].batch[" + batchIndex + "].variables"
            );
            Assertions.assertEquals(
                    new HashSet<>(
                            Arrays.asList(values)
                                    .stream()
                                    .map(it -> it instanceof byte[] ? Arrays.toString((byte[])it) : it)
                                    .collect(Collectors.toList())
                    ),
                    new HashSet<>(
                            execution.getVariables(batchIndex)
                                    .stream()
                                    .map(it -> it instanceof byte[] ? Arrays.toString((byte[])it) : it)
                                    .collect(Collectors.toList())
                    ),
                    "statements[" + index + "].batch[" + batchIndex + "].variables"
            );
        }

        public void variables(Consumer<List<Object>> block) {
            block.accept(execution.getVariables(0));
        }
    }

    protected static class ThrowableDSL {

        private final Throwable throwable;

        ThrowableDSL(Throwable throwable) {
            this.throwable = throwable;
        }

        @SuppressWarnings("unchecked")
        public <T extends Throwable> T type(Class<T> type) {
            Assertions.assertTrue(type.isAssignableFrom(throwable.getClass()));
            return (T)throwable;
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

    protected static class EntityDSL {

        private final int index;

        private final MutationResultItem<?> item;

        EntityDSL(int index, MutationResultItem<?> item) {
            this.index = index;
            this.item = item;
        }

        public void original(String json) {
            Assertions.assertEquals(
                    json.replace("--->", ""),
                    item.getOriginalEntity().toString(),
                    "originalEntities[" + index + "]"
            );
        }

        public void modified(String json) {
            Assertions.assertEquals(
                    json.replace("--->", ""),
                    item.getModifiedEntity().toString(),
                    "modifiedEntities[" + index + "]"
            );
        }
    }
}
