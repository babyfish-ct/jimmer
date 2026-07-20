package org.babyfish.jimmer.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class JdbcRecorder {

    private final Connection rawConnection;

    private final List<ProxyRecorder<PreparedStatement>> statements = new ArrayList<>();

    private final List<ProxyRecorder<ResultSet>> resultSets = new ArrayList<>();

    public JdbcRecorder(Connection rawConnection) {
        this.rawConnection = rawConnection;
    }

    public Connection connection() {
        return ProxyRecorder
                .of(Connection.class)
                .delegatesTo(rawConnection)
                .handles("prepareStatement", (method, args) ->
                        statement((PreparedStatement) ProxyRecorder.invokeTarget(rawConnection, method, args))
                )
                .proxy();
    }

    public List<Object[]> statementCalls(String methodName) {
        List<Object[]> calls = new ArrayList<>();
        for (ProxyRecorder<PreparedStatement> statement : statements) {
            calls.addAll(statement.calls(methodName));
        }
        return calls;
    }

    public List<Object[]> resultSetCalls(String methodName) {
        List<Object[]> calls = new ArrayList<>();
        for (ProxyRecorder<ResultSet> resultSet : resultSets) {
            calls.addAll(resultSet.calls(methodName));
        }
        return calls;
    }

    public List<Integer> fetchSizes() {
        List<Integer> fetchSizes = new ArrayList<>();
        for (Object[] args : statementCalls("setFetchSize")) {
            fetchSizes.add((Integer) args[0]);
        }
        return fetchSizes;
    }

    public List<Integer> queryTimeouts() {
        List<Integer> queryTimeouts = new ArrayList<>();
        for (Object[] args : statementCalls("setQueryTimeout")) {
            queryTimeouts.add((Integer) args[0]);
        }
        return queryTimeouts;
    }

    public int statementCloseCount() {
        return statementCalls("close").size();
    }

    public int resultSetCloseCount() {
        return resultSetCalls("close").size();
    }

    private PreparedStatement statement(PreparedStatement rawStatement) {
        ProxyRecorder<PreparedStatement> recorder = ProxyRecorder
                .of(PreparedStatement.class)
                .delegatesTo(rawStatement)
                .handles("executeQuery", (method, args) ->
                        resultSet((ResultSet) ProxyRecorder.invokeTarget(rawStatement, method, args))
                );
        statements.add(recorder);
        return recorder.proxy();
    }

    private ResultSet resultSet(ResultSet rawResultSet) {
        ProxyRecorder<ResultSet> recorder = ProxyRecorder
                .of(ResultSet.class)
                .delegatesTo(rawResultSet);
        resultSets.add(recorder);
        return recorder.proxy();
    }
}
