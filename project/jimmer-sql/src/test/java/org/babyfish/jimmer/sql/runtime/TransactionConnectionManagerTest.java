package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionConnectionManagerTest {

    private StringBuilder builder;

    @BeforeEach
    public void initialize() {
        builder = new StringBuilder();
    }

    /*
     * REQUIRED
     */

    @Test
    public void testRequiredOnNothing() {
        new CM(builder).test(TransactionalConnectionManager.Propagation.REQUIRED);
        assertLog(
                "open:con-1\n" +
                        "    start:con-1\n" +
                        "        business-layer-1\n" +
                        "    commit:con-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testRequiredOnConnection() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.SUPPORTS,
                TransactionalConnectionManager.Propagation.REQUIRED
        );
        assertLog(
                "open:con-1\n" +
                        "    pre-business-layer-1\n" +
                        "    start:con-1\n" +
                        "        business-layer-2\n" +
                        "    commit:con-1\n" +
                        "    abort:con-1\n" +
                        "    post-business-layer-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testRequiredOnTransaction() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.REQUIRED,
                TransactionalConnectionManager.Propagation.REQUIRED
        );
        assertLog(
                "open:con-1\n" +
                        "    start:con-1\n" +
                        "        pre-business-layer-1\n" +
                        "        business-layer-2\n" +
                        "        post-business-layer-1\n" +
                        "    commit:con-1\n" +
                        "close:con-1\n"
        );
    }

    /*
     * REQUIRES_NEW
     */

    @Test
    public void testRequiresNewOnNothing() {
        new CM(builder).test(TransactionalConnectionManager.Propagation.REQUIRES_NEW);
        assertLog(
                "open:con-1\n" +
                        "    start:con-1\n" +
                        "        business-layer-1\n" +
                        "    commit:con-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testRequiresNewOnConnection() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.SUPPORTS,
                TransactionalConnectionManager.Propagation.REQUIRES_NEW
        );
        assertLog(
                "open:con-1\n" +
                        "    pre-business-layer-1\n" +
                        "    open:con-2\n" +
                        "        start:con-2\n" +
                        "            business-layer-2\n" +
                        "        commit:con-2\n" +
                        "    close:con-2\n" +
                        "    post-business-layer-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testRequiresNewOnTransaction() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.REQUIRED,
                TransactionalConnectionManager.Propagation.REQUIRES_NEW
        );
        assertLog(
                "open:con-1\n" +
                        "    start:con-1\n" +
                        "        pre-business-layer-1\n" +
                        "        open:con-2\n" +
                        "            start:con-2\n" +
                        "                business-layer-2\n" +
                        "            commit:con-2\n" +
                        "        close:con-2\n" +
                        "        post-business-layer-1\n" +
                        "    commit:con-1\n" +
                        "close:con-1\n"
        );
    }

    /*
     * SUPPORTS
     */

    @Test
    public void testSupportsOnNothing() {
        new CM(builder).test(TransactionalConnectionManager.Propagation.SUPPORTS);
        assertLog(
                "open:con-1\n" +
                        "    business-layer-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testSupportsOnConnection() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.SUPPORTS,
                TransactionalConnectionManager.Propagation.SUPPORTS
        );
        assertLog(
                "open:con-1\n" +
                        "    pre-business-layer-1\n" +
                        "    business-layer-2\n" +
                        "    post-business-layer-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testSupportsOnTransaction() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.REQUIRED,
                TransactionalConnectionManager.Propagation.SUPPORTS
        );
        assertLog(
                "open:con-1\n" +
                        "    start:con-1\n" +
                        "        pre-business-layer-1\n" +
                        "        business-layer-2\n" +
                        "        post-business-layer-1\n" +
                        "    commit:con-1\n" +
                        "close:con-1\n"
        );
    }

    /*
     * NOT_SUPPORTED
     */

    @Test
    public void testNotSupportedOnNothing() {
        new CM(builder).test(TransactionalConnectionManager.Propagation.NOT_SUPPORTED);
        assertLog(
                "open:con-1\n" +
                        "    business-layer-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testNotSupportedOnConnection() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.SUPPORTS,
                TransactionalConnectionManager.Propagation.NOT_SUPPORTED
        );
        assertLog(
                "open:con-1\n" +
                        "    pre-business-layer-1\n" +
                        "    business-layer-2\n" +
                        "    post-business-layer-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testNotSupportedOnTransaction() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.REQUIRED,
                TransactionalConnectionManager.Propagation.NOT_SUPPORTED
        );
        assertLog(
                "open:con-1\n" +
                        "    start:con-1\n" +
                        "        pre-business-layer-1\n" +
                        "        open:con-2\n" +
                        "            business-layer-2\n" +
                        "        close:con-2\n" +
                        "        post-business-layer-1\n" +
                        "    commit:con-1\n" +
                        "close:con-1\n"
        );
    }

    /*
     * MANDATORY
     */

    @Test
    public void testMandatoryOnNothing() {
        ExecutionException ex = Assertions.assertThrows(ExecutionException.class, () -> {
            new CM(builder).test(TransactionalConnectionManager.Propagation.MANDATORY);
        });
        Assertions.assertEquals(
                "The transaction propagation is \"MANDATORY\" but there is no transaction context",
                ex.getMessage()
        );
        assertLog("");
    }

    @Test
    public void testMandatoryOnConnection() {
        ExecutionException ex = Assertions.assertThrows(ExecutionException.class, () -> {
            new CM(builder).test(
                    TransactionalConnectionManager.Propagation.SUPPORTS,
                    TransactionalConnectionManager.Propagation.MANDATORY
            );
        });
        Assertions.assertEquals(
                "The transaction propagation is \"MANDATORY\" but there is no transaction context",
                ex.getMessage()
        );
        assertLog(
                "open:con-1\n" +
                        "    pre-business-layer-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testMandatoryOnTransaction() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.REQUIRED,
                TransactionalConnectionManager.Propagation.MANDATORY
        );
        assertLog(
                "open:con-1\n" +
                        "    start:con-1\n" +
                        "        pre-business-layer-1\n" +
                        "        business-layer-2\n" +
                        "        post-business-layer-1\n" +
                        "    commit:con-1\n" +
                        "close:con-1\n"
        );
    }

    /*
     * NEVER
     */

    @Test
    public void testNeverOnNothing() {
        new CM(builder).test(TransactionalConnectionManager.Propagation.NEVER);
        assertLog(
                "open:con-1\n" +
                        "    business-layer-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testNeverOnConnection() {
        new CM(builder).test(
                TransactionalConnectionManager.Propagation.SUPPORTS,
                TransactionalConnectionManager.Propagation.NEVER
        );
        assertLog(
                "open:con-1\n" +
                        "    pre-business-layer-1\n" +
                        "    business-layer-2\n" +
                        "    post-business-layer-1\n" +
                        "close:con-1\n"
        );
    }

    @Test
    public void testNeverOnTransaction() {
        ExecutionException ex = Assertions.assertThrows(ExecutionException.class, () -> {
            new CM(builder).test(
                    TransactionalConnectionManager.Propagation.REQUIRED,
                    TransactionalConnectionManager.Propagation.NEVER
            );
        });
        Assertions.assertEquals(
                "The transaction propagation is \"NEVER\" but there is already a transaction context",
                ex.getMessage()
        );
        assertLog(
                "open:con-1\n" +
                        "    start:con-1\n" +
                        "        pre-business-layer-1\n" +
                        "    rollback:con-1\n" +
                        "close:con-1\n"
        );
    }

    /*
     * Private members
     */

    private void assertLog(String content) {
        Assertions.assertEquals(content, builder.toString());
    }

    private static Connection createConnection(int id) {
        return (Connection) Proxy.newProxyInstance(
                TransactionConnectionManagerTest.class.getClassLoader(),
                new Class<?>[]{ Connection.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();
                        switch (methodName) {
                            case "toString":
                                return "con-" + id;
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            default:
                                return null;
                        }
                    }
                }
        );
    }

    private static class CM extends AbstractTransactionalConnectionManager {

        private final StringBuilder builder;

        private int idSequence;

        private int depth;

        CM(StringBuilder builder) {
            this.builder = builder;
        }

        void test(TransactionalConnectionManager.Propagation ... propagations) {
            testImpl(propagations, 0);
        }

        private void testImpl(TransactionalConnectionManager.Propagation[] propagations, int index) {
            TransactionalConnectionManager.Propagation propagation = propagations[index];
            executeTransaction(propagation, con -> {
                if (index + 1 < propagations.length) {
                    log("pre-business-layer-" + (index + 1));
                    testImpl(propagations, index + 1);
                    log("post-business-layer-" + (index + 1));
                } else {
                    log("business-layer-" + (index + 1));
                }
                return null;
            });
        }

        @Override
        protected Connection openConnection() throws SQLException {
            Connection con = createConnection(++idSequence);
            log("open:" + con);
            ++depth;
            return con;
        }

        @Override
        protected void closeConnection(Connection con) throws SQLException {
            --depth;
            log("close:" + con);
        }

        @Override
        protected void startTransaction(Connection con) throws SQLException {
            log("start:" + con);
            ++depth;
        }

        @Override
        protected void commitTransaction(Connection con) throws SQLException {
            --depth;
            log("commit:" + con);
        }

        @Override
        protected void rollbackTransaction(Connection con) throws SQLException {
            --depth;
            log("rollback:" + con);
        }

        @Override
        protected void abortTransaction(Connection con) throws SQLException {
            log("abort:" + con);
        }

        private void log(String line) {
            for (int i = depth; i > 0; --i) {
                builder.append("    ");
            }
            builder.append(line).append('\n');
        }
    }
}
