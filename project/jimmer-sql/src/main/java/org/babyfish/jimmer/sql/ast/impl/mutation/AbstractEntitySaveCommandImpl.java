package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;

abstract class AbstractEntitySaveCommandImpl implements AbstractEntitySaveCommand {

    final JSqlClient sqlClient;

    final Connection con;

    final Data data;

    AbstractEntitySaveCommandImpl(JSqlClient sqlClient, Connection con, Data data) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.data = data != null ? data.freeze() : new Data(sqlClient).freeze();
    }

    @SuppressWarnings("unchecked")
    @Override
    public AbstractEntitySaveCommand configure(Consumer<Cfg> block) {
        Data newData = new Data(data);
        block.accept(newData);
        if (newData.mode == SaveMode.UPSERT &&
                newData.keyPropMultiMap.isEmpty() &&
                !newData.autoAttachingAll &&
                newData.dissociateActionMap.isEmpty() &&
                newData.autoAttachingSet.isEmpty()) {
            return this;
        }
        return create(newData);
    }

    abstract AbstractEntitySaveCommand create(Data data);

    static class Data implements Cfg {

        private final JSqlClient sqlClient;

        private final Triggers triggers;

        private boolean frozen;

        private SaveMode mode;

        private Map<ImmutableType, Set<ImmutableProp>> keyPropMultiMap;

        private boolean autoAttachingAll;

        private Set<ImmutableProp> autoAttachingSet;

        private Map<ImmutableProp, DissociateAction> dissociateActionMap;

        Data(JSqlClient sqlClient) {
            this.sqlClient = sqlClient;
            this.triggers = sqlClient.getTriggerType() == TriggerType.BINLOG_ONLY ?
                    null :
                    sqlClient.getTriggers(true);
            this.mode = SaveMode.UPSERT;
            this.keyPropMultiMap = new LinkedHashMap<>();
            this.autoAttachingSet = new LinkedHashSet<>();
            this.dissociateActionMap = new LinkedHashMap<>();
        }

        Data(Data base) {
            this.sqlClient = base.sqlClient;
            this.triggers = base.triggers;
            this.mode = SaveMode.UPSERT;
            this.keyPropMultiMap = new LinkedHashMap<>(base.keyPropMultiMap);
            this.autoAttachingAll = base.autoAttachingAll;
            this.autoAttachingSet = new LinkedHashSet<>(base.autoAttachingSet);
            this.dissociateActionMap = new LinkedHashMap<>(base.dissociateActionMap);
        }

        public JSqlClient getSqlClient() {
            return sqlClient;
        }

        public Triggers getTriggers() {
            return triggers;
        }

        public SaveMode getMode() {
            return mode;
        }

        public Set<ImmutableProp> getKeyProps(ImmutableType type) {
            Set<ImmutableProp> keyProps = keyPropMultiMap.get(type);
            if (keyProps != null) {
                return keyProps;
            }
            return type.getKeyProps();
        }

        public boolean isAutoAttachingProp(ImmutableProp prop) {
            return autoAttachingAll || autoAttachingSet.contains(prop);
        }

        public DissociateAction getDissociateAction(ImmutableProp prop) {
            DissociateAction action = dissociateActionMap.get(prop);
            return action != null ? action : prop.getDissociateAction();
        }

        Map<ImmutableProp, DissociateAction> dissociateActionMap() {
            return dissociateActionMap;
        }

        @Override
        public Cfg setMode(SaveMode mode) {
            validate();
            this.mode = Objects.requireNonNull(mode, "mode cannot be null");
            return this;
        }

        @Override
        public Cfg setKeyProps(ImmutableProp ... props) {
            validate();
            ImmutableType type = null;
            Set<ImmutableProp> set = new LinkedHashSet<>();
            for (ImmutableProp prop : props) {
                if (prop != null) {
                    if (prop.isId()) {
                        throw new IllegalArgumentException(
                                "'" + prop + "' cannot be key property because it is id property"
                        );
                    } else if (prop.isVersion()) {
                        throw new IllegalArgumentException(
                                "'" + prop + "' cannot be key property because it is version property"
                        );
                    } else if (prop.isAssociation(TargetLevel.ENTITY) || !(prop.getStorage() instanceof Column)) {
                        throw new IllegalArgumentException(
                                "'" + prop + "' cannot be key property because it is not a scalar property with storage"
                        );
                    } else if (prop.isNullable()) {
                        throw new IllegalArgumentException(
                                "'" + prop + "' cannot be key property because it is nullable"
                        );
                    }
                    if (type == null) {
                        type = prop.getDeclaringType();
                    } else if (type != prop.getDeclaringType()) {
                        throw new IllegalArgumentException("all key properties must belong to one type");
                    }
                    set.add(prop);
                }
            }
            if (type != null) {
                keyPropMultiMap.put(type, set);
            }
            return this;
        }

        @Override
        public Cfg setAutoAttachingAll() {
            autoAttachingAll = true;
            return this;
        }

        @Override
        public Cfg setAutoAttaching(ImmutableProp prop) {
            validate();
            autoAttachingSet.add(prop);
            return this;
        }

        @Override
        public Cfg setDissociateAction(ImmutableProp prop, DissociateAction dissociateAction) {
            validate();

            if (!prop.isReference(TargetLevel.ENTITY) || !(prop.getStorage() instanceof Column)) {
                throw new IllegalArgumentException("'" + prop + "' must be an reference property bases on foreign key");
            }
            if (dissociateAction == DissociateAction.SET_NULL && !prop.isNullable()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is not nullable so that it does not support 'on delete set null'"
                );
            }
            dissociateActionMap.put(prop, dissociateAction);
            return this;
        }

        public Data freeze() {
            if (!frozen) {
                keyPropMultiMap = Collections.unmodifiableMap(keyPropMultiMap);
                autoAttachingSet = Collections.unmodifiableSet(autoAttachingSet);
                dissociateActionMap = Collections.unmodifiableMap(dissociateActionMap);
                frozen = true;
            }
            return this;
        }

        private void validate() {
            if (frozen) {
                throw new IllegalStateException("The current configuration is frozen");
            }
        }
    }

    private static class KeyPropCfgImpl<T extends Table<?>> implements KeyPropCfg<T> {

        private Class<T> tableType;

        private List<ImmutableProp> props = new ArrayList<>();

        @SuppressWarnings("unchecked")
        KeyPropCfgImpl(Class<T> tableType) {
            this.tableType = tableType;
        }

        public List<ImmutableProp> getProps() {
            return props;
        }

        @Override
        public KeyPropCfg<T> add(ImmutableProp prop) {
            props.add(prop);
            return this;
        }

        @Override
        public KeyPropCfg<T> add(TypedProp<?, ?> prop) {
            return add(prop.unwrap());
        }
    }
}
