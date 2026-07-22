package org.babyfish.jimmer.benchmark;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

final class MockJdbc {

    private MockJdbc() {
    }

    static Connection connection(Object[][] rows) {
        ThreadLocal<ResultSetHandler> resultSets = ThreadLocal.withInitial(() -> new ResultSetHandler(rows));
        PreparedStatement statement = proxy(
                PreparedStatement.class,
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.startsWith("set") || name.equals("close")) {
                        return null;
                    }
                    if (name.equals("executeQuery")) {
                        ResultSetHandler resultSet = resultSets.get();
                        resultSet.reset();
                        return resultSet.proxy;
                    }
                    return objectMethod(proxy, method, args);
                }
        );
        return proxy(
                Connection.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        return statement;
                    }
                    if (method.getName().equals("close")) {
                        return null;
                    }
                    return objectMethod(proxy, method, args);
                }
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "toString":
                return "MockJdbc(" + proxy.getClass().getInterfaces()[0].getSimpleName() + ')';
            case "hashCode":
                return System.identityHashCode(proxy);
            case "equals":
                return proxy == args[0];
            case "isClosed":
                return false;
            case "unwrap":
                return null;
            case "isWrapperFor":
                return false;
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }

    private static final class ResultSetHandler implements InvocationHandler {

        private final Object[][] rows;

        private final ResultSet proxy;

        private int rowIndex;

        private boolean wasNull;

        private ResultSetHandler(Object[][] rows) {
            this.rows = rows;
            proxy = MockJdbc.proxy(ResultSet.class, this);
        }

        private void reset() {
            rowIndex = -1;
            wasNull = false;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            switch (name) {
                case "next":
                    return ++rowIndex < rows.length;
                case "getObject":
                    return value(args);
                case "getString":
                    return value(args);
                case "getLong":
                    return number(args).longValue();
                case "getInt":
                    return number(args).intValue();
                case "getBigDecimal":
                    return value(args);
                case "wasNull":
                    return wasNull;
                case "close":
                    return null;
                default:
                    return objectMethod(proxy, method, args);
            }
        }

        private Number number(Object[] args) {
            Object value = value(args);
            return value != null ? (Number) value : 0;
        }

        private Object value(Object[] args) {
            Object value = rows[rowIndex][(Integer) args[0] - 1];
            wasNull = value == null;
            return value;
        }
    }
}
