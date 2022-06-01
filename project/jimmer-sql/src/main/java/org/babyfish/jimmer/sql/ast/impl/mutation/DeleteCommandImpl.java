package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.DeleteAction;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.runtime.Converters;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

class DeleteCommandImpl implements DeleteCommand {

    private SqlClient sqlClient;

    private ImmutableType immutableType;

    private Collection<?> ids;

    private Data data;

    public DeleteCommandImpl(
            SqlClient sqlClient,
            ImmutableType immutableType,
            Collection<?> ids
    ) {
        Class<?> idClass = immutableType.getIdProp().getElementClass();
        for (Object id : ids) {
            if (Converters.tryConvert(id, idClass) == null) {
                throw new IllegalArgumentException(
                        "The type of \"" +
                                immutableType.getIdProp() +
                                "\" must be \"" +
                                idClass.getName() +
                                "\""
                );
            }
        }
        this.sqlClient = sqlClient;
        this.immutableType = immutableType;
        this.ids = ids;
        this.data = new Data(sqlClient).freeze();
    }

    public DeleteCommandImpl(
            DeleteCommandImpl base,
            Data data
    ) {
        this.sqlClient = base.sqlClient;
        this.immutableType = base.immutableType;
        this.ids = base.ids;
        this.data = data.freeze();
    }

    @Override
    public DeleteCommand configure(Consumer<Cfg> block) {
        Data newData = new Data(this.data);
        block.accept(newData);
        if (newData.deleteActionMap.isEmpty()) {
            return this;
        }
        return new DeleteCommandImpl(this, newData);
    }

    @Override
    public DeleteResult execute() {
        return sqlClient
                .getConnectionManager()
                .execute(this::execute);
    }

    @Override
    public DeleteResult execute(Connection con) {
        Deleter deleter = new Deleter(data, con);
        deleter.addPreHandleInput(immutableType, ids);
        return deleter.execute();
    }

    static class Data implements Cfg {

        private SqlClient sqlClient;

        private Map<ImmutableProp, DeleteAction> deleteActionMap;

        private boolean frozen;

        Data(SqlClient sqlClient) {
            this.sqlClient = sqlClient;
            this.deleteActionMap = new LinkedHashMap<>();
        }

        Data(SqlClient sqlClient, Map<ImmutableProp, DeleteAction> deleteActionMap) {
            this.sqlClient = sqlClient;
            if (deleteActionMap != null) {
                this.deleteActionMap = new LinkedHashMap<>(deleteActionMap);
            } else {
                this.deleteActionMap = new LinkedHashMap<>();
            }
        }

        Data(Data base) {
            this.sqlClient = base.sqlClient;
            this.deleteActionMap = new LinkedHashMap<>(base.deleteActionMap);
        }

        public SqlClient getSqlClient() {
            return sqlClient;
        }

        public DeleteAction getDeleteAction(ImmutableProp prop) {
            DeleteAction action = deleteActionMap.get(prop);
            return action != null ? action : prop.getDeleteAction();
        }

        public Data freeze() {
            if (!frozen) {
                deleteActionMap = Collections.unmodifiableMap(deleteActionMap);
                frozen = true;
            }
            return this;
        }

        @Override
        public Cfg setDeleteAction(ImmutableProp prop, DeleteAction deleteAction) {
            if (frozen) {
                throw new IllegalStateException("The configuration is frozen");
            }

            if (!prop.isReference() || !(prop.getStorage() instanceof Column)) {
                throw new IllegalArgumentException("'" + prop + "' must be an reference property bases on foreign key");
            }
            if (deleteAction == DeleteAction.SET_NULL && !prop.isNullable()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is not nullable so that it does not support 'on delete set null'"
                );
            }
            deleteActionMap.put(prop, deleteAction);
            return this;
        }

        @Override
        public Cfg setDeleteAction(
                Class<?> entityType,
                String prop,
                DeleteAction deleteAction
        ) {
            ImmutableType immutableType = ImmutableType.get(entityType);
            ImmutableProp immutableProp = immutableType.getProps().get(prop);
            if (immutableProp == null || !immutableProp.isReference()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is not reference property of \"" + entityType.getName() + "\""
                );
            }
            return setDeleteAction(immutableProp, deleteAction);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Table<?>> Cfg setDeleteAction(
                Class<T> tableType,
                Function<T, Table<?>> block,
                DeleteAction deleteAction
        ) {
            return setDeleteAction(ImmutableProps.join(tableType, block), deleteAction);
        }
    }
}
