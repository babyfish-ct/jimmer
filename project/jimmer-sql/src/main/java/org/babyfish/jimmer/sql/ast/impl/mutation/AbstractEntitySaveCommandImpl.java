package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.KeyMatcher;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.OneToMany;
import org.babyfish.jimmer.sql.OneToOne;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.util.*;

abstract class AbstractEntitySaveCommandImpl
        extends AbstractCommandImpl
        implements AbstractEntitySaveCommand, SaveCommandImplementor {

    AbstractEntitySaveCommandImpl(Cfg cfg) {
        super(cfg);
    }

    @Override
    final SaveOptions createOptions() {
        return new OptionsImpl(cfg);
    }

    static class ModeCfg extends Cfg {

        final SaveMode mode;

        public ModeCfg(Cfg prev, SaveMode mode) {
            super(prev);
            this.mode = mode != null ? mode : SaveMode.UPSERT;
        }
    }

    static class AssociatedModeCfg extends Cfg {

        final MapNode<ImmutableProp, AssociatedSaveMode> mapNode;

        @Nullable
        final AssociatedSaveMode defaultMode;

        public AssociatedModeCfg(Cfg prev, @Nullable AssociatedSaveMode defaultMode) {
            super(prev);
            AssociatedModeCfg p = prev.as(AssociatedModeCfg.class);
            this.mapNode = p != null ? p.mapNode : null;
            this.defaultMode = defaultMode != null ? defaultMode : AssociatedSaveMode.REPLACE;
        }

        public AssociatedModeCfg(Cfg prev, ImmutableProp prop, AssociatedSaveMode mode) {
            super(prev);
            if (!prop.isAssociation(TargetLevel.PERSISTENT)) {
                throw new IllegalArgumentException(
                        "Cannot specify the associated save mode for the property \"" +
                                prop +
                                "\" that is not an ORM association"
                );
            }
            AssociatedModeCfg p = prev.as(AssociatedModeCfg.class);
            this.mapNode = new MapNode<>(p != null ? p.mapNode : null, prop, mode);
            this.defaultMode = p != null ? p.defaultMode : AssociatedSaveMode.REPLACE;
        }
    }

    static class KeyGroupsCfg extends Cfg {

        final MapNode<ImmutableType, Map<String, Set<ImmutableProp>>> mapNode;

        public KeyGroupsCfg(Cfg prev, String group, Collection<ImmutableProp> keyProps) {
            super(prev);
            if (group == null) {
                group = "";
            }
            if (keyProps.isEmpty()) {
                throw new IllegalArgumentException("keyProps cannot be empty");
            }
            ImmutableType type = null;
            Map<String, Set<ImmutableProp>> map = new LinkedHashMap<>();
            Set<ImmutableProp> set = new LinkedHashSet<>();
            for (ImmutableProp prop : keyProps) {
                if (prop != null) {
                    if (prop.isId()) {
                        throw new IllegalArgumentException(
                                "'" + prop + "' of key group \"" + group + "\" cannot be key property because it is id property"
                        );
                    } else if (prop.isVersion()) {
                        throw new IllegalArgumentException(
                                "'" + prop + "' of key group \"" + group + "\" cannot be key property because it is version property"
                        );
                    } else if (!prop.isColumnDefinition()) {
                        throw new IllegalArgumentException(
                                "'" + prop  + "' of key group \"" + group + "\" cannot be key property because it is not property with column definition"
                        );
                    }
                    if (type == null) {
                        type = prop.getDeclaringType();
                    } else if (type != prop.getDeclaringType()) {
                        throw new IllegalArgumentException(
                                "all key properties of key group \"" + group + "\"must belong to one type"
                        );
                    }
                    set.add(prop);
                }
            }
            map.put(group, set);
            KeyGroupsCfg p = prev.as(KeyGroupsCfg.class);
            this.mapNode = new MapNode<>(p != null ? p.mapNode : null, type, map);
        }
    }

    static class UpsertMaskCfg extends Cfg {

        final MapNode<ImmutableType, UpsertMask<?>> mapNode;

        UpsertMaskCfg(Cfg prev, UpsertMask<?> mask) {
            super(prev);
            Objects.requireNonNull(mask, "mask cannot be null");
            UpsertMaskCfg p = prev.as(UpsertMaskCfg.class);
            this.mapNode = new MapNode<>(p != null ? p.mapNode : null, mask.getType(), mask);
        }
    }

    static class IdOnlyAutoCheckingCfg extends Cfg {

        final MapNode<ImmutableProp, Boolean> mapNode;

        final boolean defaultValue;

        public IdOnlyAutoCheckingCfg(Cfg prev, boolean defaultValue) {
            super(prev);
            IdOnlyAutoCheckingCfg p = prev.as(IdOnlyAutoCheckingCfg.class);
            this.mapNode = p != null ? p.mapNode : null;
            this.defaultValue = defaultValue;
        }

        public IdOnlyAutoCheckingCfg(Cfg prev, ImmutableProp prop, boolean lock) {
            super(prev);
            if (!prop.isAssociation(TargetLevel.PERSISTENT)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" is not association property"
                );
            }
            IdOnlyAutoCheckingCfg p = prev.as(IdOnlyAutoCheckingCfg.class);
            this.mapNode = new MapNode<>(p != null ? p.mapNode : null, prop, lock);
            this.defaultValue = p != null && p.defaultValue;
        }
    }

    static class IdOnlyAsReferenceCfg extends Cfg {

        final MapNode<ImmutableProp, Boolean> mapNode;

        final boolean defaultValue;

        IdOnlyAsReferenceCfg(Cfg prev, boolean defaultValue) {
            super(prev);
            IdOnlyAsReferenceCfg p = prev.as(IdOnlyAsReferenceCfg.class);
            this.mapNode = p != null ? p.mapNode : null;
            this.defaultValue = defaultValue;
        }

        IdOnlyAsReferenceCfg(Cfg prev, ImmutableProp prop, boolean asReference) {
            super(prev);
            if (!prop.isAssociation(TargetLevel.PERSISTENT)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" is not association property"
                );
            }
            IdOnlyAsReferenceCfg p = prev.as(IdOnlyAsReferenceCfg.class);
            this.mapNode = new MapNode<>(p != null ? p.mapNode : null, prop, asReference);
            this.defaultValue = p == null || p.defaultValue;
        }
    }

    static class KeyOnlyAsReferenceCfg extends Cfg {

        final MapNode<ImmutableProp, Boolean> mapNode;

        final boolean defaultValue;

        public KeyOnlyAsReferenceCfg(Cfg prev, boolean defaultValue) {
            super(prev);
            KeyOnlyAsReferenceCfg p = prev.as(KeyOnlyAsReferenceCfg.class);
            this.mapNode = p != null ? p.mapNode : null;
            this.defaultValue = defaultValue;
        }

        public KeyOnlyAsReferenceCfg(Cfg prev, ImmutableProp prop, boolean asReference) {
            super(prev);
            if (!prop.isAssociation(TargetLevel.PERSISTENT)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" is not association property"
                );
            }
            KeyOnlyAsReferenceCfg p = prev.as(KeyOnlyAsReferenceCfg.class);
            this.mapNode = new MapNode<>(p != null ? p.mapNode : null, prop, asReference);
            this.defaultValue = p != null && p.defaultValue;
        }
    }

    static class TargetTransferModeCfg extends Cfg {

        final MapNode<ImmutableProp, TargetTransferMode> mapNode;

        final TargetTransferMode defaultMode;

        public TargetTransferModeCfg(Cfg prev, TargetTransferMode defaultMode) {
            super(prev);
            TargetTransferModeCfg p = prev.as(TargetTransferModeCfg.class);
            this.mapNode = p != null ? p.mapNode : null;
            this.defaultMode = defaultMode;
        }

        public TargetTransferModeCfg(Cfg prev, ImmutableProp prop, TargetTransferMode mode) {
            super(prev);
            Annotation annotation = prop.getAssociationAnnotation();
            if (annotation instanceof OneToOne) {
                OneToOne oneToOne = (OneToOne) annotation;
                if (!oneToOne.mappedBy().isEmpty()) {
                    throw new IllegalArgumentException(
                            "In order to set target transfer mode, the one-to-one property \"" +
                                    prop +
                                    "\" must be inverse property(mappedBy)"
                    );
                }
            } else if (!(annotation instanceof OneToMany)) {
                throw new IllegalArgumentException(
                        "Cannot set the target transfer mode of the property \"" +
                                prop +
                                "\" because it is neither one-to-one property and one-to-many property"
                );
            }
            TargetTransferModeCfg p = prev.as(TargetTransferModeCfg.class);
            this.mapNode = new MapNode<>(p != null ? p.mapNode : null, prop, mode);
            this.defaultMode = p != null ? p.defaultMode : TargetTransferMode.AUTO;
        }
    }

     static class PessimisticLockCfg extends Cfg {

         final MapNode<ImmutableType, Boolean> mapNode;

         final Boolean defaultValue;

        public PessimisticLockCfg(Cfg prev, boolean defaultValue) {
            super(prev);
            PessimisticLockCfg p = prev.as(PessimisticLockCfg.class);
            this.mapNode = p != null ? p.mapNode : null;
            this.defaultValue = defaultValue;
        }

         public PessimisticLockCfg(Cfg prev, Class<?> entityType, boolean checking) {
             super(prev);
             ImmutableType type = ImmutableType.get(entityType);
             PessimisticLockCfg p = prev.as(PessimisticLockCfg.class);
             this.mapNode = new MapNode<>(p != null ? p.mapNode : null, type, checking);
             this.defaultValue = p != null && p.defaultValue;
         }
    }

    static class OptimisticLockLambdaCfg extends Cfg {

        final MapNode<ImmutableType, UnloadedVersionBehavior> behaviorMapNode;

        final MapNode<ImmutableType, UserOptimisticLock<Object, Table<Object>>> lamdadaMapNode;

        public OptimisticLockLambdaCfg(
                Cfg prev,
                ImmutableType type,
                UnloadedVersionBehavior behavior,
                UserOptimisticLock<Object, Table<Object>> block
        ) {
            super(prev);
            if (!type.isEntity()) {
                throw new IllegalArgumentException(
                        "Cannot set the optimistic lock lambda for the type \"" +
                                type +
                                "\" because it is not entity"
                );
            }
            OptimisticLockLambdaCfg p = prev.as(OptimisticLockLambdaCfg.class);
            this.behaviorMapNode = new MapNode<>(p != null ? p.behaviorMapNode : null, type, behavior);
            this.lamdadaMapNode = new MapNode<>(p != null ? p.lamdadaMapNode : null, type, block);
        }
    }

    static class ExceptionTranslatorCfg extends Cfg {

        final ListNode<ExceptionTranslator<?>> listNode;

        ExceptionTranslatorCfg(Cfg prev, ExceptionTranslator<?> translator) {
            super(prev);
            ExceptionTranslatorCfg p = prev.as(ExceptionTranslatorCfg.class);
            this.listNode = new ListNode<>(p != null ? p.listNode : null, translator);
        }
    }

    static final class OptionsImpl implements SaveOptions {

        private final JSqlClientImplementor sqlClient;

        private final Object argument;

        private final Connection con;

        private final SaveMode mode;

        private final AssociatedSaveMode associatedMode;

        private final Map<ImmutableProp, AssociatedSaveMode> associatedModeMap;

        private final DeleteMode deleteMode;

        private final int maxCommandJoinCount;

        private final Map<ImmutableType, KeyMatcher> keyMatcherMap;

        private final Map<ImmutableType, UpsertMask<?>> upsertMaskMap;

        private final Map<ImmutableProp, Boolean> autoCheckingMap;

        private final boolean autoCheckingAll;

        private final Map<ImmutableProp, Boolean> idOnlyAsReferenceMap;

        private final boolean idOnlyAsReferenceAll;

        private final Map<ImmutableProp, Boolean> keyOnlyAsReferenceMap;

        private final boolean keyOnlyAsReferenceAll;

        private final Map<ImmutableProp, DissociateAction> dissociateActionMap;

        private final Map<ImmutableProp, TargetTransferMode> targetTransferModeMap;

        private final TargetTransferMode targetTransferModeAll;

        private final Map<ImmutableType, Boolean> pessimisticLockMap;

        private final boolean pessimisticLockAll;

        private final Map<ImmutableType, UnloadedVersionBehavior> optimisticLockBehaviorMap;

        private final Map<ImmutableType, UserOptimisticLock<Object, Table<Object>>> optimisticLockLambdaMap;

        private final boolean dumbBatchAcceptable;

        private final boolean constraintViolationTranslatable;

        private final ExceptionTranslator<Exception> exceptionTranslator;

        private final boolean transactionRequired;

        OptionsImpl(Cfg cfg) {
            RootCfg rootCfg = cfg.as(RootCfg.class);
            ConnectionCfg connectionCfg = cfg.as(ConnectionCfg.class);
            ModeCfg modeCfg = cfg.as(ModeCfg.class);
            AssociatedModeCfg associatedModeCfg = cfg.as(AssociatedModeCfg.class);
            DeleteModeCfg deleteModeCfg = cfg.as(DeleteModeCfg.class);
            MaxCommandJoinCountCfg maxCommandJoinCountCfg = cfg.as(MaxCommandJoinCountCfg.class);
            KeyGroupsCfg keyPropsCfg = cfg.as(KeyGroupsCfg.class);
            UpsertMaskCfg upsertMaskCfg = cfg.as(UpsertMaskCfg.class);
            IdOnlyAutoCheckingCfg idOnlyAutoCheckingCfg = cfg.as(IdOnlyAutoCheckingCfg.class);
            IdOnlyAsReferenceCfg idOnlyAsReferenceCfg = cfg.as(IdOnlyAsReferenceCfg.class);
            KeyOnlyAsReferenceCfg keyOnlyAsReferenceCfg = cfg.as(KeyOnlyAsReferenceCfg.class);
            DissociationActionCfg dissociationActionCfg = cfg.as(DissociationActionCfg.class);
            TargetTransferModeCfg targetTransferModeCfg = cfg.as(TargetTransferModeCfg.class);
            PessimisticLockCfg pessimisticLockCfg = cfg.as(PessimisticLockCfg.class);
            OptimisticLockLambdaCfg optimisticLockLambdaCfg = cfg.as(OptimisticLockLambdaCfg.class);
            DumbBatchAcceptableCfg dumbBatchAcceptableCfg = cfg.as(DumbBatchAcceptableCfg.class);
            ConstraintViolationTranslatableCfg constraintViolationTranslatableCfg =
                    cfg.as(ConstraintViolationTranslatableCfg.class);
            ExceptionTranslatorCfg exceptionTranslatorCfg = cfg.as(ExceptionTranslatorCfg.class);
            TransactionRequiredCfg transactionRequiredCfg = cfg.as(TransactionRequiredCfg.class);

            assert rootCfg != null;
            this.sqlClient = rootCfg.sqlClient;
            this.argument = rootCfg.argument;
            this.con = connectionCfg != null ? connectionCfg.con : null;
            this.mode = modeCfg != null ? modeCfg.mode : SaveMode.UPSERT;
            this.associatedModeMap = MapNode.toMap(associatedModeCfg, it -> it.mapNode);;
            this.associatedMode = associatedModeCfg != null ?
                    associatedModeCfg.defaultMode :
                    AssociatedSaveMode.REPLACE;
            this.deleteMode = deleteModeCfg != null ?
                    deleteModeCfg.mode :
                    DeleteMode.AUTO;
            this.maxCommandJoinCount = maxCommandJoinCountCfg != null ?
                    maxCommandJoinCountCfg.maxCommandJoinCount :
                    sqlClient.getMaxCommandJoinCount();
            this.keyMatcherMap = keyMatcherMap(MapNode.toMap(keyPropsCfg, it -> it.mapNode));
            this.upsertMaskMap = MapNode.toMap(upsertMaskCfg, it -> it.mapNode);
            this.autoCheckingMap = MapNode.toMap(idOnlyAutoCheckingCfg, it -> it.mapNode);
            this.autoCheckingAll = idOnlyAutoCheckingCfg != null && idOnlyAutoCheckingCfg.defaultValue;
            this.idOnlyAsReferenceMap = MapNode.toMap(idOnlyAsReferenceCfg, it -> it.mapNode);
            this.idOnlyAsReferenceAll = idOnlyAsReferenceCfg == null || idOnlyAsReferenceCfg.defaultValue;
            this.keyOnlyAsReferenceMap = MapNode.toMap(keyOnlyAsReferenceCfg, it -> it.mapNode);
            this.keyOnlyAsReferenceAll = keyOnlyAsReferenceCfg != null && keyOnlyAsReferenceCfg.defaultValue;
            this.dissociateActionMap = MapNode.toMap(dissociationActionCfg, it -> it.mapNode);
            this.targetTransferModeMap = MapNode.toMap(targetTransferModeCfg, it -> it.mapNode);
            this.targetTransferModeAll = targetTransferModeCfg != null ?
                    targetTransferModeCfg.defaultMode :
                    TargetTransferMode.AUTO;
            this.pessimisticLockMap = MapNode.toMap(pessimisticLockCfg, it -> it.mapNode);
            this.pessimisticLockAll = pessimisticLockCfg != null ?
                    pessimisticLockCfg.defaultValue :
                    false;
            this.optimisticLockBehaviorMap = MapNode.toMap(optimisticLockLambdaCfg, it -> it.behaviorMapNode);
            this.optimisticLockLambdaMap = MapNode.toMap(optimisticLockLambdaCfg, it -> it.lamdadaMapNode);
            this.dumbBatchAcceptable = dumbBatchAcceptableCfg != null && dumbBatchAcceptableCfg.acceptable;
            this.constraintViolationTranslatable = constraintViolationTranslatableCfg != null ?
                    constraintViolationTranslatableCfg.translatable :
                    sqlClient.isConstraintViolationTranslatable();
            if (exceptionTranslatorCfg != null) {
                ExceptionTranslator<Exception> defaultTranslator = sqlClient.getExceptionTranslator();
                Collection<ExceptionTranslator<?>> translators;
                if (defaultTranslator == null) {
                    translators = ListNode.toList(exceptionTranslatorCfg, it -> it.listNode);
                } else {
                    translators = new ArrayList<>();
                    translators.add(defaultTranslator);
                    translators.addAll(ListNode.toList(exceptionTranslatorCfg, it -> it.listNode));
                }
                this.exceptionTranslator = ExceptionTranslator.of(translators);
            } else {
                this.exceptionTranslator = sqlClient.getExceptionTranslator();
            }
            this.transactionRequired = transactionRequiredCfg != null ?
                    transactionRequiredCfg.required :
                    sqlClient.isMutationTransactionRequired();
        }

        @Override
        public JSqlClientImplementor getSqlClient() {
            return sqlClient;
        }

        public <T> T getArument() {
            return (T)argument;
        }

        @Override
        public Connection getConnection() {
            return con;
        }

        @Override
        public Triggers getTriggers() {
            return sqlClient.getTriggerType() == TriggerType.BINLOG_ONLY ? null : sqlClient.getTriggers();
        }

        @Override
        public SaveMode getMode() {
            return mode;
        }

        @Override
        public AssociatedSaveMode getAssociatedMode(ImmutableProp prop) {
            AssociatedSaveMode mode = associatedModeMap.get(prop);
            return mode != null ? mode : associatedMode;
        }

        @Override
        public DeleteMode getDeleteMode() {
            return deleteMode;
        }

        @Override
        public int getMaxCommandJoinCount() {
            return maxCommandJoinCount;
        }

        @Override
        public KeyMatcher getKeyMatcher(ImmutableType type) {
            KeyMatcher keyMatcher = keyMatcherMap.get(type);
            if (keyMatcher != null) {
                return keyMatcher;
            }
            return type.getKeyMatcher();
        }

        @Override
        @Nullable
        public UpsertMask<?> getUpsertMask(ImmutableType type) {
            return upsertMaskMap.get(type);
        }

        public boolean isAutoCheckingProp(ImmutableProp prop) {
            if (Boolean.FALSE.equals(autoCheckingMap.get(prop))) {
                return false;
            }
            switch (sqlClient.getIdOnlyTargetCheckingLevel()) {
                case ALL:
                    return true;
                case FAKE:
                    if (!prop.isTargetForeignKeyReal(sqlClient.getMetadataStrategy())) {
                        return true;
                    }
                    break;
            }
            return autoCheckingAll || Boolean.TRUE.equals(autoCheckingMap.get(prop));
        }

        @Override
        public boolean isIdOnlyAsReference(ImmutableProp prop) {
            Boolean value = idOnlyAsReferenceMap.get(prop);
            if (value != null) {
                return value;
            }
            return idOnlyAsReferenceAll;
        }

        public boolean isKeyOnlyAsReference(ImmutableProp prop) {
            Boolean value = keyOnlyAsReferenceMap.get(prop);
            if (value != null) {
                return value;
            }
            return keyOnlyAsReferenceAll;
        }

        @Override
        public DissociateAction getDissociateAction(ImmutableProp prop) {
            DissociateAction action = dissociateActionMap.get(prop);
            return action != null ? action : prop.getDissociateAction();
        }

        @Override
        public boolean isTargetTransferable(ImmutableProp prop) {
            TargetTransferMode mode = targetTransferModeMap.getOrDefault(prop, targetTransferModeAll);
            switch (mode) {
                case ALLOWED:
                    return true;
                case NOT_ALLOWED:
                    return false;
                default:
                    switch (prop.getTargetTransferMode()) {
                        case ALLOWED:
                            return true;
                        case NOT_ALLOWED:
                            return false;
                        default:
                            return sqlClient.isTargetTransferable();
                    }
            }
        }

        @Override
        public boolean isPessimisticLocked(ImmutableType type) {
            Boolean value = pessimisticLockMap.get(type);
            return value != null ? value : pessimisticLockAll;
        }

        @Override
        @NotNull
        public UnloadedVersionBehavior getUnloadedVersionBehavior(ImmutableType type) {
            return optimisticLockBehaviorMap.getOrDefault(type, UnloadedVersionBehavior.IGNORE);
        }

        @Override
        public UserOptimisticLock<Object, Table<Object>> getUserOptimisticLock(ImmutableType type) {
            return optimisticLockLambdaMap.get(type);
        }

        @Override
        public boolean isBatchForbidden() {
            return sqlClient.isBatchForbidden(dumbBatchAcceptable);
        }

        @Override
        public boolean isConstraintViolationTranslatable() {
            return constraintViolationTranslatable;
        }

        @Override
        public @Nullable ExceptionTranslator<Exception> getExceptionTranslator() {
            return exceptionTranslator;
        }

        @Override
        public boolean isTransactionRequired() {
            return transactionRequired;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    sqlClient,
                    argument,
                    mode,
                    associatedMode,
                    associatedModeMap,
                    targetTransferModeMap,
                    targetTransferModeAll,
                    pessimisticLockMap,
                    pessimisticLockAll,
                    deleteMode,
                    keyMatcherMap,
                    autoCheckingAll,
                    autoCheckingMap,
                    dissociateActionMap
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OptionsImpl)) return false;
            OptionsImpl other = (OptionsImpl) o;
            return sqlClient == other.sqlClient &&
                    autoCheckingAll == other.autoCheckingAll &&
                    associatedMode == other.associatedMode &&
                    pessimisticLockAll == other.pessimisticLockAll &&
                    mode == other.mode &&
                    deleteMode == other.deleteMode &&
                    Objects.equals(argument, other.argument) &&
                    targetTransferModeMap.equals(other.targetTransferModeMap) &&
                    targetTransferModeAll == other.targetTransferModeAll &&
                    associatedModeMap.equals(other.associatedModeMap) &&
                    keyMatcherMap.equals(other.keyMatcherMap) &&
                    autoCheckingMap.equals(other.autoCheckingMap) &&
                    dissociateActionMap.equals(other.dissociateActionMap) &&
                    pessimisticLockMap.equals(other.pessimisticLockMap);
        }

        @Override
        public String toString() {
            return "SaveOptions{" +
                    "sqlClient=" + sqlClient +
                    ", mode=" + mode +
                    ", associatedMode=" + associatedMode +
                    ", associatedModeMap=" + associatedModeMap +
                    ", targetTransferableMap=" + targetTransferModeMap +
                    ", targetTransferModeAll=" + targetTransferModeAll +
                    ", pessimisticLockMap" + pessimisticLockMap +
                    ", pessimisticLockAll" + pessimisticLockAll +
                    ", deleteMode=" + deleteMode +
                    ", keyMatcherMap=" + keyMatcherMap +
                    ", autoCheckingMap=" + autoCheckingMap +
                    ", autoCheckingAll=" + autoCheckingAll +
                    ", dissociateActionMap=" + dissociateActionMap +
                    ", optimisticLockLambdaMap=" + optimisticLockLambdaMap +
                    '}';
        }

        private Map<ImmutableType, KeyMatcher> keyMatcherMap(
                Map<ImmutableType, Map<String, Set<ImmutableProp>>> map
        ) {
            if (map.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<ImmutableType, KeyMatcher> keyMatcherMap = new LinkedHashMap<>();
            for (Map.Entry<ImmutableType, Map<String, Set<ImmutableProp>>> e : map.entrySet()) {
                ImmutableType type = e.getKey();
                Map<String, Set<ImmutableProp>> groupMap = new LinkedHashMap<>(type.getKeyMatcher().toMap());
                groupMap.putAll(e.getValue());
                keyMatcherMap.put(type, KeyMatcher.of(type, groupMap));
            }
            return keyMatcherMap;
        }
    }
}