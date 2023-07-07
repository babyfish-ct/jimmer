package org.babyfish.jimmer.sql.cache.transaction;

import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.Book;
import org.h2.Driver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class H2TransactionCacheOperatorTest extends AbstractTransactionCacheOperatorTest {

    private Connection _con;

    private Connection connection() throws SQLException {
        if (_con != null) {
            return _con;
        }
        return _con = new Driver().connect("jdbc:h2:mem:trans-cache-operator", null);
    }

    @AfterEach
    public void closeConnection() throws SQLException {
        _con.close();
    }

    @Override
    protected DataSource dataSource() {
        return new InMemDataSource();
    }

    @Override
    protected Dialect dialect() {
        return new H2Dialect();
    }

    private class InMemDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return new UncloseableConnection(connection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return new UncloseableConnection(connection());
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {

        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {

        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return null;
        }
    }

    private static class UncloseableConnection implements Connection {

        private final Connection raw;

        private UncloseableConnection(Connection raw) {
            this.raw = raw;
        }

        @Override
        public void close() throws SQLException {
            // Do nothing
        }

        @Override
        public Statement createStatement() throws SQLException {
            return raw.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return raw.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return raw.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return raw.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            raw.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return raw.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            raw.commit();
        }

        @Override
        public void rollback() throws SQLException {
            raw.rollback();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return raw.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return raw.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            raw.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return raw.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            raw.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return raw.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            raw.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return raw.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return raw.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            raw.clearWarnings();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return raw.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return raw.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return raw.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return raw.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            raw.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            raw.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return raw.getHoldability();
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return raw.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return raw.setSavepoint(name);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            raw.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            raw.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return raw.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return raw.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return raw.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return raw.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return raw.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return raw.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException {
            return raw.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException {
            return raw.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return raw.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return raw.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return raw.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            raw.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            raw.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return raw.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return raw.getClientInfo();
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return raw.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return raw.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            raw.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return raw.getSchema();
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            raw.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            raw.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return raw.getNetworkTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return raw.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return raw.isWrapperFor(iface);
        }
    }
}
