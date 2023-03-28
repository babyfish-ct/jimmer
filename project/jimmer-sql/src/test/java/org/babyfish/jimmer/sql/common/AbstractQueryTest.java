package org.babyfish.jimmer.sql.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class AbstractQueryTest extends AbstractTest {

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .registerModule(new ImmutableModule())
                    .registerModule(new JavaTimeModule());

    private List<?> rows;

    private int maxStatementIndex = -1;

    protected <R> void executeAndExpect(
            TypedRootQuery<R> query,
            Consumer<QueryTestContext<R>> block
    ) {
        clearExecutions();
        rows = null;
        maxStatementIndex = -1;
        jdbc(con -> {
            if (rows == null) {
                rows = query.execute(con);
            }
        });
        block.accept(new QueryTestContext<>(0));
        Assertions.assertEquals(
                maxStatementIndex + 1,
                getExecutions().size(),
                "statement count"
        );
    }

    protected <T> void anyAndExpect(
            Executable<T> executable,
            Consumer<QueryTestContext<T>> block
    ) {
        connectAndExpect(executable::execute, block);
    }

    protected <T> void connectAndExpect(
            Function<Connection, T> func,
            Consumer<QueryTestContext<T>> block
    ) {
        clearExecutions();
        rows = null;
        maxStatementIndex = -1;
        jdbc(con -> {
            if (rows == null) {
                Object result = func.apply(con);
                if (result instanceof List<?>) {
                    rows = (List<?>) result;
                } else {
                    rows = Collections.singletonList(result);
                }
            }
        });
        block.accept(new QueryTestContext<>(0));
        Assertions.assertEquals(
                maxStatementIndex + 1,
                getExecutions().size(),
                "statement count"
        );
    }

    protected class QueryTestContext<R> {

        private int index;

        QueryTestContext(int index) {
            this.index = index;
        }

        public QueryTestContext<R> statement(int index) {
            return new QueryTestContext<>(index);
        }

        public QueryTestContext<R> sql(String sql) {
            maxStatementIndex = Math.max(maxStatementIndex, index);
            List<Execution> executions = getExecutions();
            Assertions.assertFalse(
                    executions.isEmpty(),
                    "Not sql history"
            );
            Assertions.assertEquals(
                    sql.replace("--->", ""),
                    executions.get(index).getSql(),
                    "statements[" + index + "].sql"
            );
            return this;
        }

        public QueryTestContext<R> variables(Object ...variables) {
            variables(Arrays.asList(variables));
            return this;
        }

        public QueryTestContext<R> variables(List<Object> variables) {
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
            return this;
        }

        @SuppressWarnings("unchecked")
        public QueryTestContext<R> rows(Consumer<List<R>> block) {
            block.accept((List<R>) rows);
            return this;
        }

        public QueryTestContext<R> rows(List<R> rows) {
            Assertions.assertEquals(
                    rows,
                    AbstractQueryTest.this.rows
            );
            return this;
        }

        @SuppressWarnings("unchecked")
        public QueryTestContext<R> row(int index, Consumer<R> consumer) {
            consumer.accept((R)rows.get(index));
            return this;
        }

        public QueryTestContext<R> rows(String json) {
            try {
                Assertions.assertEquals(
                        json.replace("--->", ""),
                        MAPPER.writeValueAsString(rows)
                );
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            return this;
        }

        public QueryTestContext<R> rows(int count) {
            Assertions.assertEquals(count, rows.size());
            return this;
        }
    }

    protected static void assertLoadState(
            Object obj,
            String ... props
    ) {
        if (obj instanceof List<?>) {
            int index = 0;
            for (Object entity : (List<?>) obj) {
                try {
                    assertLoadState(entity, props);
                } catch (AssertionFailedError err) {
                    throw new AssertionFailedError(
                            "list[" +
                                    index +
                                    "]: " +
                                    err.getMessage(),
                            err.getExpected(),
                            err.getActual()
                    );
                }
                index++;
            }
        } else {
            ImmutableSpi spi = (ImmutableSpi) obj;
            List<String> propNameList = Arrays.asList(props);
            for (String propName : propNameList) {
                if (!spi.__type().getProps().containsKey(propName)) {
                    Assertions.fail("No property \"" + propName + "\" in \"" + spi.__type() + "\"");
                }
            }
            for (ImmutableProp prop : spi.__type().getProps().values()) {
                if (prop.getIdViewBaseProp() == null && prop.getDependencies().isEmpty()) {
                    Assertions.assertEquals(
                            propNameList.contains(prop.getName()),
                            spi.__isLoaded(prop.getId()),
                            "Bad load state of prop \"" + prop + "\""
                    );
                }
            }
        }
    }

    protected static void expect(String json, Object o) {
        TestUtils.expect(json, o);
    }
}
