package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.sql.Column;
import org.babyfish.jimmer.meta.sql.MiddleTable;
import org.babyfish.jimmer.sql.OnDeleteAction;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class Deleter {

    private DeleteCommandImpl.Data data;

    private Connection con;

    private Map<ImmutableType, Set<Object>> preHandleIdInputMap =
            new LinkedHashMap<>();

    private Map<ImmutableType, Set<Object>> postHandleIdInputMap =
            new LinkedHashMap<>();

    private Map<String, Integer> affectedRowCountMap =
            new LinkedHashMap<>();

    public Deleter(DeleteCommandImpl.Data data, Connection con) {
        this.data = data;
        this.con = con;
    }

    public void addPreHandleInput(ImmutableType type, Collection<?> ids) {
        Set<Object> idSet = preHandleIdInputMap.computeIfAbsent(
                type,
                t -> new LinkedHashSet<>()
        );
        for (Object id : ids) {
            if (id != null) {
                idSet.add(id);
            }
        }
    }

    private void addPostHandleInput(ImmutableType type, Collection<?> ids) {
        Set<Object> idSet = postHandleIdInputMap.computeIfAbsent(
                type,
                t -> new LinkedHashSet<>()
        );
        for (Object id : ids) {
            if (id != null) {
                idSet.add(id);
            }
        }
    }

    private void addOutput(String tableName, int affectedRowCount) {
        Integer old = affectedRowCountMap.get(tableName);
        if (old == null) {
            affectedRowCountMap.put(tableName, affectedRowCount);
        } else {
            affectedRowCountMap.put(tableName, old + affectedRowCount);
        }
    }

    public DeleteResult execute() {
        while (!preHandleIdInputMap.isEmpty() || !postHandleIdInputMap.isEmpty()) {
            while (!preHandleIdInputMap.isEmpty()) {
                preHandle();
            }
            postHandle();
        }
        return new DeleteResult(affectedRowCountMap);
    }

    private void preHandle() {
        Map<ImmutableType, Set<Object>> idMultiMap = preHandleIdInputMap;
        preHandleIdInputMap = new LinkedHashMap<>();
        for (Map.Entry<ImmutableType, Set<Object>> e : idMultiMap.entrySet()) {
            preHandle(e.getKey(), e.getValue());
        }
    }

    private void preHandle(ImmutableType immutableType, Collection<Object> ids) {
        if (ids.isEmpty()) {
            return;
        }
        for (ImmutableProp prop : immutableType.getProps().values()) {
            ImmutableProp mappedByProp = prop.getMappedBy();
            MiddleTable middleTable = null;
            if (mappedByProp != null) {
                if (mappedByProp.getStorage() instanceof MiddleTable) {
                    middleTable = ((MiddleTable) mappedByProp.getStorage()).getInverse();
                }
            } else if (prop.getStorage() instanceof MiddleTable) {
                middleTable = (MiddleTable) prop.getStorage();
            }
            if (middleTable != null) {
                deleteFromMiddleTable(middleTable, ids);
            }
            if (prop.isEntityList() && mappedByProp != null && mappedByProp.isReference()) {
                OnDeleteAction onDeleteAction = data.getOnDeleteAction(mappedByProp);
                if (onDeleteAction == OnDeleteAction.SET_NULL ||
                        (onDeleteAction == OnDeleteAction.SMART && mappedByProp.isNullable())) {
                    updateChildTable(mappedByProp, ids);
                } else {
                    tryDeleteFromChildTable(mappedByProp, ids);
                }
            }
        }
        addPostHandleInput(immutableType, ids);
    }

    private void deleteFromMiddleTable(MiddleTable middleTable, Collection<Object> ids) {
        SqlBuilder builder = new SqlBuilder(data.getSqlClient());
        builder.sql("delete from ");
        builder.sql(middleTable.getTableName());
        builder.sql(" where ");
        builder.sql(middleTable.getJoinColumnName());
        builder.sql(" in(");
        String separator = "";
        for (Object id : ids) {
            builder.sql(separator);
            separator = ", ";
            builder.variable(id);
        }
        builder.sql(")");
        Tuple2<String, List<Object>> sqlResult = builder.build();
        int affectedRowCount = data
                .getSqlClient()
                .getExecutor()
                .execute(
                        con,
                        sqlResult._1(),
                        sqlResult._2(),
                        PreparedStatement::executeUpdate
                );
        addOutput(middleTable.getTableName(), affectedRowCount);
    }

    private void updateChildTable(
            ImmutableProp manyToOneProp,
            Collection<Object> ids
    ) {
        ImmutableType childType = manyToOneProp.getDeclaringType();
        String fkColumnName = ((Column)manyToOneProp.getStorage()).getName();
        SqlBuilder builder = new SqlBuilder(data.getSqlClient());
        builder
                .sql("update ")
                .sql(childType.getTableName())
                .sql(" set ")
                .sql(fkColumnName)
                .sql(" = null where ")
                .sql(fkColumnName)
                .sql(" in(");
        String separator = "";
        for (Object id : ids) {
            builder.sql(separator);
            separator = ", ";
            builder.variable(id);
        }
        builder.sql(")");
        Tuple2<String, List<Object>> sqlResult = builder.build();
        int affectedRowCount = data
                .getSqlClient()
                .getExecutor()
                .execute(
                        con,
                        sqlResult._1(),
                        sqlResult._2(),
                        PreparedStatement::executeUpdate
                );
        addOutput(childType.getTableName(), affectedRowCount);
    }

    private void tryDeleteFromChildTable(ImmutableProp manyToOneProp, Collection<?> ids) {
        ImmutableType childType = manyToOneProp.getDeclaringType();
        String fkColumnName = ((Column)manyToOneProp.getStorage()).getName();
        SqlBuilder builder = new SqlBuilder(data.getSqlClient());
        builder
                .sql("select ")
                .sql(childType.getIdProp().getName())
                .sql(" from ")
                .sql(childType.getTableName())
                .sql(" where ")
                .sql(fkColumnName)
                .sql(" in(");
        String separator = "";
        for (Object id : ids) {
            builder.sql(separator);
            separator = ", ";
            builder.variable(id);
        }
        builder.sql(")");
        Tuple2<String, List<Object>> sqlResult = builder.build();
        List<Object> childIds = data
                .getSqlClient()
                .getExecutor()
                .execute(
                        con,
                        sqlResult._1(),
                        sqlResult._2(),
                        stmt -> {
                            List<Object> values = new ArrayList<>();
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    values.add(rs.getObject(1));
                                }
                            }
                            return values;
                        }
                );
        if (!childIds.isEmpty()) {
            if (data.getOnDeleteAction(manyToOneProp) != OnDeleteAction.CASCADE) {
                throw new ExecutionException(
                        "Cannot delete entities whose type are \"" +
                                manyToOneProp.getTargetType().getJavaClass().getName() +
                                "\" because there are some child entities whose type are \"" +
                                manyToOneProp.getDeclaringType().getJavaClass().getName() +
                                "\", these child entities use the association property \"" +
                                manyToOneProp +
                                "\" to reference current entities."
                );
            }
            addPreHandleInput(childType, childIds);
            preHandle();
        }
    }

    private void postHandle() {
        Map<ImmutableType, Set<Object>> idMultiMap = postHandleIdInputMap;
        postHandleIdInputMap = new LinkedHashMap<>();
        for (Map.Entry<ImmutableType, Set<Object>> e : idMultiMap.entrySet()) {
            deleteFromSelfTable(e.getKey(), e.getValue());
        }
    }

    private void deleteFromSelfTable(ImmutableType type, Collection<Object> ids) {
        String fkColumnName = ((Column)type.getIdProp().getStorage()).getName();
        SqlBuilder builder = new SqlBuilder(data.getSqlClient());
        builder.sql("delete from ");
        builder.sql(type.getTableName());
        builder.sql(" where ");
        builder.sql(fkColumnName);
        builder.sql(" in(");
        String separator = "";
        for (Object id : ids) {
            builder.sql(separator);
            separator = ", ";
            builder.variable(id);
        }
        builder.sql(")");
        Tuple2<String, List<Object>> sqlResult = builder.build();
        int affectedRowCount = data
                .getSqlClient()
                .getExecutor()
                .execute(
                        con,
                        sqlResult._1(),
                        sqlResult._2(),
                        PreparedStatement::executeUpdate
                );
        addOutput(type.getTableName(), affectedRowCount);
    }
}
