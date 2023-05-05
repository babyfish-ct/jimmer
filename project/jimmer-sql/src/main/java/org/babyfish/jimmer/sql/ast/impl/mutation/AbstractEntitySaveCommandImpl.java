package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

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

    @Override
    public AbstractEntitySaveCommand configure(Consumer<Cfg> block) {
        Data newData = new Data(data);
        block.accept(newData);
        if (data.equals(newData)) {
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

        private DeleteMode deleteMode;

        private Map<ImmutableType, Set<ImmutableProp>> keyPropMultiMap;

        private boolean autoAttachingAll;

        private Set<ImmutableProp> autoAttachingSet;

        private boolean autoCheckingAll;

        private Set<ImmutableProp> autoCheckingSet;

        private boolean appendOnlyAll;

        private Set<ImmutableProp> appendOnlySet;

        private Map<ImmutableProp, DissociateAction> dissociateActionMap;

        private boolean pessimisticLock;

        Data(JSqlClient sqlClient) {
            this.sqlClient = sqlClient;
            this.triggers = sqlClient.getTriggerType() == TriggerType.BINLOG_ONLY ?
                    null :
                    sqlClient.getTriggers(true);
            this.mode = SaveMode.UPSERT;
            this.deleteMode = DeleteMode.AUTO;
            this.keyPropMultiMap = new LinkedHashMap<>();
            this.autoAttachingSet = new HashSet<>();
            this.autoCheckingSet = new HashSet<>();
            this.appendOnlySet = new HashSet<>();
            this.dissociateActionMap = new LinkedHashMap<>();
            this.pessimisticLock = false;
        }

        Data(Data base) {
            this.sqlClient = base.sqlClient;
            this.triggers = base.triggers;
            this.mode = SaveMode.UPSERT;
            this.deleteMode = base.deleteMode;
            this.keyPropMultiMap = new LinkedHashMap<>(base.keyPropMultiMap);
            this.autoAttachingAll = base.autoAttachingAll;
            this.autoAttachingSet = new HashSet<>(base.autoAttachingSet);
            this.autoCheckingAll = base.autoCheckingAll;
            this.autoCheckingSet = new HashSet<>(base.autoCheckingSet);
            this.appendOnlyAll = base.appendOnlyAll;
            this.appendOnlySet = base.appendOnlySet;
            this.dissociateActionMap = new LinkedHashMap<>(base.dissociateActionMap);
            this.pessimisticLock = base.pessimisticLock;
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

        public DeleteMode getDeleteMode() { return deleteMode; }

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

        public boolean isAutoCheckingProp(ImmutableProp prop) {
            return autoCheckingAll || autoCheckingSet.contains(prop);
        }

        public boolean isAppendOnly(ImmutableProp prop) {
            return appendOnlyAll || appendOnlySet.contains(prop);
        }

        public DissociateAction getDissociateAction(ImmutableProp prop) {
            DissociateAction action = dissociateActionMap.get(prop);
            return action != null ? action : prop.getDissociateAction();
        }

        Map<ImmutableProp, DissociateAction> dissociateActionMap() {
            return dissociateActionMap;
        }

        boolean isPessimisticLockRequired() {
            return pessimisticLock;
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
                    } else if (prop.isAssociation(TargetLevel.PERSISTENT) || !(prop.isColumnDefinition())) {
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
        public Cfg setAutoIdOnlyTargetCheckingAll() {
            autoCheckingAll = true;
            return this;
        }

        @Override
        public Cfg setAutoIdOnlyTargetChecking(ImmutableProp prop) {
            autoCheckingSet.add(prop);
            return this;
        }

        @Override
        public Cfg setAppendOnly(ImmutableProp prop) {
            appendOnlySet.add(prop);
            return this;
        }

        @Override
        public Cfg setAppendOnlyAll() {
            appendOnlyAll = true;
            return this;
        }

        @Override
        public Cfg setDissociateAction(ImmutableProp prop, DissociateAction dissociateAction) {
            validate();

            if (!prop.isReference(TargetLevel.PERSISTENT) || !(prop.isColumnDefinition())) {
                throw new IllegalArgumentException("'" + prop + "' must be an reference property bases on foreign key");
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
        public Cfg setPessimisticLock(boolean pessimisticLock) {
            this.pessimisticLock = pessimisticLock;
            return this;
        }

        @Override
        public Cfg setDeleteMode(DeleteMode mode) {
            this.deleteMode = Objects.requireNonNull(mode, "mode cannot be null");
            return this;
        }

        public Data freeze() {
            if (!frozen) {
                keyPropMultiMap = Collections.unmodifiableMap(keyPropMultiMap);
                autoAttachingSet = Collections.unmodifiableSet(autoAttachingSet);
                autoCheckingSet = Collections.unmodifiableSet(autoCheckingSet);
                dissociateActionMap = Collections.unmodifiableMap(dissociateActionMap);
                frozen = true;
            }
            return this;
        }

        @Override
        public int hashCode() {
            return Objects.hash(sqlClient, triggers, frozen, mode, deleteMode, keyPropMultiMap, autoAttachingAll, autoAttachingSet, autoCheckingAll, autoCheckingSet, dissociateActionMap, pessimisticLock);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return frozen == data.frozen && autoAttachingAll == data.autoAttachingAll && autoCheckingAll == data.autoCheckingAll && pessimisticLock == data.pessimisticLock && sqlClient.equals(data.sqlClient) && Objects.equals(triggers, data.triggers) && mode == data.mode && deleteMode == data.deleteMode && keyPropMultiMap.equals(data.keyPropMultiMap) && autoAttachingSet.equals(data.autoAttachingSet) && autoCheckingSet.equals(data.autoCheckingSet) && dissociateActionMap.equals(data.dissociateActionMap);
        }

        @Override
        public String toString() {
            return "Data{" +
                    "sqlClient=" + sqlClient +
                    ", triggers=" + triggers +
                    ", frozen=" + frozen +
                    ", mode=" + mode +
                    ", deleteMode=" + deleteMode +
                    ", keyPropMultiMap=" + keyPropMultiMap +
                    ", autoAttachingAll=" + autoAttachingAll +
                    ", autoAttachingSet=" + autoAttachingSet +
                    ", autoCheckingAll=" + autoCheckingAll +
                    ", autoCheckingSet=" + autoCheckingSet +
                    ", dissociateActionMap=" + dissociateActionMap +
                    ", pessimisticLock=" + pessimisticLock +
                    '}';
        }

        private void validate() {
            if (frozen) {
                throw new IllegalStateException("The current configuration is frozen");
            }
        }
    }
}
