package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.runtime.Converters;

import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;

public class DeleteCommandImpl implements DeleteCommand {

    private final JSqlClient sqlClient;

    private final Connection con;

    private final ImmutableType immutableType;

    private final Collection<?> ids;

    private final Data data;

    public DeleteCommandImpl(
            JSqlClient sqlClient,
            Connection con,
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
        this.con = con;
        this.immutableType = immutableType;
        this.ids = ids;
        this.data = new Data(sqlClient).freeze();
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
        if (newData.dissociateActionMap.isEmpty()) {
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
                binLogOnly ? null : new MutationCache(sqlClient),
                binLogOnly ? null : new MutationTrigger(),
                new HashMap<>()
        );
        deleter.addPreHandleInput(immutableType, ids);
        return deleter.execute();
    }

    static class Data implements Cfg {

        private JSqlClient sqlClient;

        private Map<ImmutableProp, DissociateAction> dissociateActionMap;

        private boolean frozen;

        Data(JSqlClient sqlClient) {
            this.sqlClient = sqlClient;
            this.dissociateActionMap = new LinkedHashMap<>();
        }

        Data(JSqlClient sqlClient, Map<ImmutableProp, DissociateAction> dissociateActionMap) {
            this.sqlClient = sqlClient;
            if (dissociateActionMap != null) {
                this.dissociateActionMap = new LinkedHashMap<>(dissociateActionMap);
            } else {
                this.dissociateActionMap = new LinkedHashMap<>();
            }
        }

        Data(Data base) {
            this.sqlClient = base.sqlClient;
            this.dissociateActionMap = new LinkedHashMap<>(base.dissociateActionMap);
        }

        public JSqlClient getSqlClient() {
            return sqlClient;
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
        public Cfg setDissociateAction(ImmutableProp prop, DissociateAction dissociateAction) {
            if (frozen) {
                throw new IllegalStateException("The configuration is frozen");
            }

            if (!prop.isReference(TargetLevel.ENTITY) || !(prop.getStorage() instanceof ColumnDefinition)) {
                throw new IllegalArgumentException("'" + prop + "' must be an entity reference property bases on foreign key");
            }
            if (dissociateAction == DissociateAction.SET_NULL && !prop.isNullable()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is not nullable so that it does not support 'on delete set null'"
                );
            }
            dissociateActionMap.put(prop, dissociateAction);
            return this;
        }
    }
}
