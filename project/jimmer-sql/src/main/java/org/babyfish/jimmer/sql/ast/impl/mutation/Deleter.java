package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class Deleter {

    private final DeleteCommandImpl.Data data;

    private final Connection con;

    private final MutationCache cache;

    private final Map<AffectedTable, Integer> affectedRowCountMap;

    private Map<ImmutableType, Set<Object>> preHandleIdInputMap =
            new LinkedHashMap<>();

    private Map<ImmutableType, Set<Object>> postHandleIdInputMap =
            new LinkedHashMap<>();

    private final Map<String, Deleter> childDeleterMap =
            new LinkedHashMap<>();

    Deleter(
            DeleteCommandImpl.Data data,
            Connection con
    ) {
        this(data, con, new MutationCache(data.getSqlClient()), new LinkedHashMap<>());
    }

    Deleter(
            DeleteCommandImpl.Data data,
            Connection con,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        this(data, con, new MutationCache(data.getSqlClient()), affectedRowCountMap);
    }

    Deleter(
            DeleteCommandImpl.Data data,
            Connection con,
            MutationCache cache
    ) {
        this(data, con, cache, new LinkedHashMap<>());
    }

    Deleter(
            DeleteCommandImpl.Data data,
            Connection con,
            MutationCache cache,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        this.data = data;
        this.con = con;
        this.cache = cache;
        this.affectedRowCountMap = affectedRowCountMap;
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

    private void addOutput(AffectedTable affectTable, int affectedRowCount) {
        affectedRowCountMap.merge(affectTable, affectedRowCount, Integer::sum);
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
            ImmutableProp middleTableProp = null;
            MiddleTable middleTable = null;
            if (mappedByProp != null) {
                if (mappedByProp.getStorage() instanceof MiddleTable) {
                    middleTableProp = mappedByProp;
                    middleTable = middleTableProp.<MiddleTable>getStorage().getInverse();
                }
            } else if (prop.getStorage() instanceof MiddleTable) {
                middleTableProp = prop;
                middleTable = middleTableProp.getStorage();
            }
            if (middleTable != null) {
                deleteFromMiddleTable(middleTableProp, middleTable, ids);
            }
            if (prop.isReferenceList(TargetLevel.ENTITY) &&
                    mappedByProp != null &&
                    mappedByProp.isReference(TargetLevel.ENTITY)
            ) {
                DissociateAction dissociateAction = data.getDissociateAction(mappedByProp);
                if (dissociateAction == DissociateAction.SET_NULL) {
                    updateChildTable(mappedByProp, ids);
                } else {
                    tryDeleteFromChildTable(prop, ids);
                }
            }
        }
        addPostHandleInput(immutableType, ids);
    }

    private void deleteFromMiddleTable(
            ImmutableProp middleTableProp,
            MiddleTable middleTable,
            Collection<Object> ids
    ) {
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
                        sqlResult.get_1(),
                        sqlResult.get_2(),
                        null,
                        PreparedStatement::executeUpdate
                );
        addOutput(AffectedTable.of(middleTableProp), affectedRowCount);
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
                        sqlResult.get_1(),
                        sqlResult.get_2(),
                        null,
                        PreparedStatement::executeUpdate
                );
        addOutput(AffectedTable.of(childType), affectedRowCount);
    }

    private void tryDeleteFromChildTable(ImmutableProp prop, Collection<?> ids) {
        ImmutableProp manyToOneProp = prop.getMappedBy();
        ImmutableType childType = manyToOneProp.getDeclaringType();
        String fkColumnName = ((Column)manyToOneProp.getStorage()).getName();
        SqlBuilder builder = new SqlBuilder(data.getSqlClient());
        builder
                .sql("select ")
                .sql(childType.getIdProp().<Column>getStorage().getName())
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
                        sqlResult.get_1(),
                        sqlResult.get_2(),
                        null,
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
            if (data.getDissociateAction(manyToOneProp) != DissociateAction.DELETE) {
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
            Deleter childDeleter = childDeleterMap.computeIfAbsent(
                    prop.getName(),
                    it -> new Deleter(data, con, affectedRowCountMap)
            );
            childDeleter.addPreHandleInput(childType, childIds);
            childDeleter.preHandle();
        }
    }

    private void postHandle() {
        childDeleterMap.values().forEach(Deleter::postHandle);
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
                        sqlResult.get_1(),
                        sqlResult.get_2(),
                        null,
                        PreparedStatement::executeUpdate
                );
        addOutput(AffectedTable.of(type), affectedRowCount);
    }
}
