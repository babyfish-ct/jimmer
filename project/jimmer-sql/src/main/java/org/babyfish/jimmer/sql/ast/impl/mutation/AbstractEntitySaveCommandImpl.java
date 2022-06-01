package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.DeleteAction;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class AbstractEntitySaveCommandImpl<C extends AbstractEntitySaveCommand<C>> implements AbstractEntitySaveCommand<C> {

    SqlClient sqlClient;

    Data data;

    AbstractEntitySaveCommandImpl(SqlClient sqlClient, Data data) {
        this.sqlClient = sqlClient;
        this.data = data != null ? data.freeze() : new Data(sqlClient).freeze();
    }

    @SuppressWarnings("unchecked")
    @Override
    public C configure(Consumer<Cfg> block) {
        Data newData = new Data(data);
        block.accept(newData);
        if (newData.mode == SaveMode.UPSERT &&
                newData.keyPropMultiMap.isEmpty() &&
                !newData.autoAttachingAll &&
                newData.deleteActionMap.isEmpty() &&
                newData.autoAttachingSet.isEmpty()) {
            return (C)this;
        }
        return create(newData);
    }

    abstract C create(Data data);

    static class Data implements Cfg {

        private SqlClient sqlClient;

        private boolean frozen;

        private SaveMode mode;

        private Map<ImmutableType, Set<ImmutableProp>> keyPropMultiMap;

        private boolean autoAttachingAll;

        private Set<ImmutableProp> autoAttachingSet;

        private Map<ImmutableProp, DeleteAction> deleteActionMap;

        Data(SqlClient sqlClient) {
            this.sqlClient = sqlClient;
            this.mode = SaveMode.UPSERT;
            this.keyPropMultiMap = new LinkedHashMap<>();
            this.autoAttachingSet = new LinkedHashSet<>();
            this.deleteActionMap = new LinkedHashMap<>();
        }

        Data(Data base) {
            this.sqlClient = base.sqlClient;
            this.mode = SaveMode.UPSERT;
            this.keyPropMultiMap = new LinkedHashMap<>(base.keyPropMultiMap);
            this.autoAttachingAll = base.autoAttachingAll;
            this.autoAttachingSet = new LinkedHashSet<>(base.autoAttachingSet);
            this.deleteActionMap = new LinkedHashMap<>(base.deleteActionMap);
        }

        public SqlClient getSqlClient() {
            return sqlClient;
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

        public DeleteAction getDeleteAction(ImmutableProp prop) {
            DeleteAction action = deleteActionMap.get(prop);
            return action != null ? action : prop.getDeleteAction();
        }

        Map<ImmutableProp, DeleteAction> deleteActionMap() {
            return deleteActionMap;
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
                    } else if (prop.isAssociation() || !(prop.getStorage() instanceof Column)) {
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
        public Cfg setKeyProps(Class<?> entityType, String... props) {
            ImmutableType type = ImmutableType.get(entityType);
            return setKeyProps(
                    Arrays.stream(props)
                            .map(type::getProp)
                            .toArray(ImmutableProp[]::new)
            );
        }

        @Override
        public <T extends Table<?>> Cfg setKeyProps(
                Class<T> tableType,
                Consumer<KeyPropCfg<T>> block
        ) {
            KeyPropCfgImpl<T> keyPropCfg = new KeyPropCfgImpl<T>(tableType);
            block.accept(keyPropCfg);
            return setKeyProps(
                    keyPropCfg.getProps().toArray(new ImmutableProp[0])
            );
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
        public Cfg setAutoAttaching(Class<?> entityType, String prop) {
            ImmutableType immutableType = ImmutableType.get(entityType);
            ImmutableProp immutableProp = immutableType.getProp(prop);
            return setAutoAttaching(immutableProp);
        }

        @Override
        public <T extends Table<?>> Cfg setAutoAttaching(
                Class<T> tableType,
                Function<T, Table<?>> block
        ) {
            return setAutoAttaching(ImmutableProps.join(tableType, block));
        }

        @Override
        public Cfg setDeleteAction(ImmutableProp prop, DeleteAction deleteAction) {
            validate();

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
            ImmutableProp immutableProp = immutableType.getProp(prop);
            return setDeleteAction(immutableProp, deleteAction);
        }

        @Override
        public <T extends Table<?>> Cfg setDeleteAction(
                Class<T> tableType,
                Function<T, Table<?>> block,
                DeleteAction deleteAction
        ) {
            return setDeleteAction(
                    ImmutableProps.join(tableType, block),
                    deleteAction
            );
        }

        public Data freeze() {
            if (!frozen) {
                keyPropMultiMap = Collections.unmodifiableMap(keyPropMultiMap);
                autoAttachingSet = Collections.unmodifiableSet(autoAttachingSet);
                deleteActionMap = Collections.unmodifiableMap(deleteActionMap);
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
        public KeyPropCfg<T> add(Function<T, PropExpression<?>> block) {
            ImmutableProp prop = ImmutableProps.get(tableType, block);
            props.add(prop);
            return this;
        }
    }
}
