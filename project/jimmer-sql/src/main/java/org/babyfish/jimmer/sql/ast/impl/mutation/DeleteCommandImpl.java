package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.OnDeleteAction;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.ast.table.Table;

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
        if (newData.onDeleteActionMap.isEmpty()) {
            return this;
        }
        return new DeleteCommandImpl(this, newData);
    }

    @Override
    public DeleteResult execute(Connection con) {
        Deleter deleter = new Deleter(data, con);
        deleter.addPreHandleInput(immutableType, ids);
        return deleter.execute();
    }

    static class Data implements Cfg {

        private SqlClient sqlClient;

        private Map<ImmutableProp, OnDeleteAction> onDeleteActionMap;

        private boolean frozen;

        Data(SqlClient sqlClient) {
            this.sqlClient = sqlClient;
            this.onDeleteActionMap = new LinkedHashMap<>();
        }

        Data(Data base) {
            this.sqlClient = base.sqlClient;
            this.onDeleteActionMap = new LinkedHashMap<>(base.onDeleteActionMap);
        }

        public SqlClient getSqlClient() {
            return sqlClient;
        }

        public OnDeleteAction getOnDeleteAction(ImmutableProp prop) {
            OnDeleteAction action = onDeleteActionMap.get(prop);
            return action != null ? action : OnDeleteAction.NONE;
        }

        public Data freeze() {
            if (!frozen) {
                onDeleteActionMap = Collections.unmodifiableMap(onDeleteActionMap);
                frozen = true;
            }
            return this;
        }

        @Override
        public Cfg setOnDeleteAction(ImmutableProp prop, OnDeleteAction onDeleteAction) {
            if (frozen) {
                throw new IllegalStateException("The configuration is frozen");
            }

            if (!prop.isReference()) {
                throw new IllegalArgumentException("'" + prop + "' is not reference property");
            }
            if (onDeleteAction == OnDeleteAction.SET_NULL && !prop.isNullable()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is not nullable so that it does not support 'on delete set null'"
                );
            }
            onDeleteActionMap.put(prop, onDeleteAction);
            return this;
        }

        @Override
        public Cfg setOnDeleteAction(
                Class<?> entityType,
                String prop,
                OnDeleteAction onDeleteAction
        ) {
            ImmutableType immutableType = ImmutableType.get(entityType);
            ImmutableProp immutableProp = immutableType.getProps().get(prop);
            if (immutableProp == null || !immutableProp.isReference()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is not reference property of \"" + entityType.getName() + "\""
                );
            }
            return setOnDeleteAction(immutableProp, onDeleteAction);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Table<?>> Cfg setOnDeleteAction(
                Class<T> tableType,
                Function<T, Table<?>> block,
                OnDeleteAction onDeleteAction
        ) {
            return setOnDeleteAction(ImmutableProps.join(tableType, block), onDeleteAction);
        }
    }
}
