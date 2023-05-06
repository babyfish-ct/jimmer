package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.runtime.Converters;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;

public class DeleteCommandImpl implements DeleteCommand {

    private final JSqlClientImplementor sqlClient;

    private final Connection con;

    private final ImmutableType immutableType;

    private final Collection<?> ids;

    private final Data data;

    public DeleteCommandImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType immutableType,
            Collection<?> ids,
            DeleteMode mode
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
        this.con = con;
        this.immutableType = immutableType;
        this.ids = ids;
        this.data = new Data(sqlClient, mode).freeze();
    }

    public DeleteCommandImpl(
            DeleteCommandImpl base,
            Data data
    ) {
        this.sqlClient = base.sqlClient;
        this.con = base.con;
        this.immutableType = base.immutableType;
        this.ids = base.ids;
        this.data = data.freeze();
    }

    @Override
    public DeleteCommand configure(Consumer<Cfg> block) {
        Data newData = new Data(this.data);
        block.accept(newData);
        if (data.equals(newData)) {
            return this;
        }
        return new DeleteCommandImpl(this, newData);
    }

    @Override
    public DeleteResult execute() {
        if (con != null) {
            return executeImpl(con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @Override
    public DeleteResult execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        if (this.con != null) {
            return executeImpl(this.con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    private DeleteResult executeImpl(Connection con) {
        boolean binLogOnly = sqlClient.getTriggerType() == TriggerType.BINLOG_ONLY;
        Deleter deleter = new Deleter(
                data,
                con,
                binLogOnly ? null : new MutationCache(sqlClient, false),
                binLogOnly ? null : new MutationTrigger(),
                new HashMap<>()
        );
        deleter.addPreHandleInput(immutableType, ids);
        return deleter.execute();
    }

    static class Data implements Cfg {

        private final JSqlClientImplementor sqlClient;

        private DeleteMode mode;

        private Map<ImmutableProp, DissociateAction> dissociateActionMap;

        private boolean frozen;

        Data(JSqlClientImplementor sqlClient, DeleteMode deleteMode) {
            this.sqlClient = sqlClient;
            this.mode = deleteMode;
            this.dissociateActionMap = new LinkedHashMap<>();
        }

        Data(JSqlClientImplementor sqlClient, DeleteMode mode, Map<ImmutableProp, DissociateAction> dissociateActionMap) {
            this.sqlClient = sqlClient;
            this.mode = mode;
            if (dissociateActionMap != null) {
                this.dissociateActionMap = new LinkedHashMap<>(dissociateActionMap);
            } else {
                this.dissociateActionMap = new LinkedHashMap<>();
            }
        }

        Data(Data base) {
            this.sqlClient = base.sqlClient;
            this.mode = base.mode;
            this.dissociateActionMap = new LinkedHashMap<>(base.dissociateActionMap);
        }

        public JSqlClientImplementor getSqlClient() {
            return sqlClient;
        }

        public DeleteMode getMode() {
            return mode;
        }

        public DissociateAction getDissociateAction(ImmutableProp prop) {
            DissociateAction action = dissociateActionMap.get(prop);
            return action != null ? action : prop.getDissociateAction();
        }

        public Data freeze() {
            if (!frozen) {
                dissociateActionMap = Collections.unmodifiableMap(dissociateActionMap);
                frozen = true;
            }
            return this;
        }

        @Override
        public Cfg setMode(DeleteMode mode) {
            this.mode = mode;
            return this;
        }

        @Override
        public Cfg setDissociateAction(ImmutableProp prop, DissociateAction dissociateAction) {
            if (frozen) {
                throw new IllegalStateException("The configuration is frozen");
            }

            if (!prop.isReference(TargetLevel.PERSISTENT) || !prop.isColumnDefinition()) {
                throw new IllegalArgumentException("'" + prop + "' must be an entity reference property bases on foreign key");
            }
            if (dissociateAction == DissociateAction.SET_NULL && !prop.isNullable()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is not nullable so that it does not support 'on delete set null'"
                );
            }
            if (dissociateAction == DissociateAction.SET_NULL && prop.isInputNotNull()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is `inputNotNull` so that it does not support 'on delete set null'"
                );
            }
            dissociateActionMap.put(prop, dissociateAction);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Data)) return false;
            Data data = (Data) o;
            return frozen == data.frozen && sqlClient.equals(data.sqlClient) && mode == data.mode && dissociateActionMap.equals(data.dissociateActionMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sqlClient, mode, dissociateActionMap, frozen);
        }

        @Override
        public String toString() {
            return "Data{" +
                    "sqlClient=" + sqlClient +
                    ", mode=" + mode +
                    ", dissociateActionMap=" + dissociateActionMap +
                    ", frozen=" + frozen +
                    '}';
        }
    }
}
