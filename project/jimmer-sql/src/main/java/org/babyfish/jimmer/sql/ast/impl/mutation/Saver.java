package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MiddleTable;

import java.sql.Connection;
import java.util.*;

public class Saver {

    private final SaveContext ctx;

    public Saver(
            SaveOptions options,
            Connection con,
            ImmutableType type
    ) {
        this(
                new SaveContext(
                        options,
                        con,
                        type
                )
        );
    }

    private Saver(SaveContext ctx) {
        this.ctx = ctx;
    }

    @SuppressWarnings("unchecked")
    public <E> SimpleSaveResult<E> save(E entity) {
        ImmutableType immutableType = ImmutableType.get(entity.getClass());
        MutationTrigger trigger = ctx.trigger;
        E newEntity = (E) Internal.produce(
                immutableType,
                entity,
                draft -> {
                    saveAllImpl(Collections.singletonList((DraftSpi) draft));
                },
                trigger == null ? null : trigger::prepareSubmit
        );
        if (trigger != null) {
            trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }
        return new SimpleSaveResult<>(
                ctx.affectedRowCountMap,
                entity,
                newEntity
        );
    }

    @SuppressWarnings("unchecked")
    public <E> BatchSaveResult<E> saveAll(Collection<E> entities) {
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyList());
        }
        ImmutableType immutableType = ImmutableType.get(entities.iterator().next().getClass());
        MutationTrigger trigger = ctx.trigger;
        List<E> newEntities = (List<E>) Internal.produceList(
                immutableType,
                entities,
                drafts -> {
                    saveAllImpl((List<DraftSpi>) drafts);
                },
                trigger == null ? null : trigger::prepareSubmit
        );
        if (trigger != null) {
            trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }
        Iterator<E> oldItr = entities.iterator();
        Iterator<E> newItr = newEntities.iterator();
        List<SimpleSaveResult<E>> results = new ArrayList<>(entities.size());
        while (oldItr.hasNext() && newItr.hasNext()) {
            results.add(
                    new SimpleSaveResult<>(
                            ctx.affectedRowCountMap,
                            oldItr.next(),
                            newItr.next()
                    )
            );
        }
        return new BatchSaveResult<>(results);
    }

    private void saveAllImpl(List<DraftSpi> drafts) {
        for (ImmutableProp prop : ctx.path.getType().getProps().values()) {
            if (prop.isReference(TargetLevel.ENTITY) && prop.isColumnDefinition()) {
                savePreAssociation(prop, drafts);
            }
        }

        PreHandler preHandler = PreHandler.of(ctx);
        for (DraftSpi draft : drafts) {
            preHandler.add(draft);
        }

        boolean detach = saveSelf(preHandler);

        for (Batch<DraftSpi> batch : preHandler.associationBatches()) {
            for (ImmutableProp prop : batch.shape().getGetterMap().keySet()) {
                if (prop.isAssociation(TargetLevel.ENTITY)) {
                    if (ctx.options.getAssociatedMode(prop) == AssociatedSaveMode.VIOLENTLY_REPLACE) {
                        clearAssociations(batch.entities(), prop);
                    }
                    setBackReference(prop, batch);
                    savePostAssociation(prop, batch, detach);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setBackReference(ImmutableProp prop, Batch<DraftSpi> batch) {
        ImmutableProp backProp = prop.getMappedBy();
        if (backProp != null && backProp.isColumnDefinition()) {
            ImmutableType parentType = prop.getDeclaringType();
            PropId idPropId = batch.shape().getType().getIdProp().getId();
            PropId propId = prop.getId();
            PropId backPropId = backProp.getId();
            for (DraftSpi draft : batch.entities()) {
                if (draft.__isLoaded(idPropId)) {
                    Object idOnlyParent = ImmutableObjects.makeIdOnly(parentType, draft.__get(idPropId));
                    Object associated = draft.__get(propId);
                    if (associated instanceof Collection<?>) {
                        for (DraftSpi child : (List<DraftSpi>) associated) {
                            child.__set(backPropId, idOnlyParent);
                        }
                    } else if (associated instanceof DraftSpi) {
                        ((DraftSpi) associated).__set(backPropId, idOnlyParent);
                    }
                }
            }
        }
    }

    private void savePreAssociation(ImmutableProp prop, List<DraftSpi> drafts) {
        Saver targetSaver = new Saver(ctx.prop(prop));
        List<DraftSpi> targets = new ArrayList<>(drafts.size());
        PropId targetPropId = prop.getId();
        for (DraftSpi draft : drafts) {
            if (draft.__isLoaded(targetPropId)) {
                DraftSpi target = (DraftSpi) draft.__get(targetPropId);
                if (target != null) {
                    targets.add(target);
                } else if (!prop.isNullable() || prop.isInputNotNull()) {
                    targetSaver.ctx.throwNullTarget();
                }
            }
        }
        if (!targets.isEmpty()) {
            targetSaver.saveAllImpl(targets);
        }
    }

    @SuppressWarnings("unchecked")
    private void savePostAssociation(
            ImmutableProp prop,
            Batch<DraftSpi> batch,
            boolean detachOtherSiblings
    ) {
        Saver targetSaver = new Saver(ctx.prop(prop));

        if (isReadOnlyMiddleTable(prop)) {
            targetSaver.ctx.throwReadonlyMiddleTable();
        }
        if (prop.isRemote() && prop.getMappedBy() != null) {
            targetSaver.ctx.throwReversedRemoteAssociation();
        }
        if (prop.getSqlTemplate() instanceof JoinTemplate) {
            targetSaver.ctx.throwUnstructuredAssociation();
        }

        List<DraftSpi> targets = new ArrayList<>(batch.entities().size());
        PropId targetPropId = prop.getId();
        for (DraftSpi draft : batch.entities()) {
            Object value = draft.__get(targetPropId);
            if (value instanceof List<?>) {
                targets.addAll((List<DraftSpi>) value);
            } else if (value != null) {
                targets.add((DraftSpi) value);
            } else if (!prop.isNullable() || prop.isInputNotNull()) {
                targetSaver.ctx.throwNullTarget();
            }
        }
        if (!targets.isEmpty()) {
            targetSaver.saveAllImpl(targets);
        }

        updateAssociations(batch, prop, detachOtherSiblings);
    }

    private boolean saveSelf(PreHandler preHandler) {
        Operator operator = new Operator(ctx);
        boolean detach = false;
        for (Batch<DraftSpi> batch : preHandler.batches()) {
            switch (batch.mode()) {
                case INSERT_ONLY:
                    operator.insert(batch);
                    break;
                case UPDATE_ONLY:
                    detach = true;
                    operator.update(
                            preHandler.originalIdObjMap(),
                            preHandler.originalkeyObjMap(),
                            batch
                    );
                    break;
                default:
                    detach = true;
                    operator.upsert(batch);
                    break;
            }
        }
        return detach;
    }

    private void clearAssociations(Collection<? extends ImmutableSpi> rows, ImmutableProp prop) {
        ChildTableOperator subOperator = null;
        MiddleTableOperator middleTableOperator = null;
        if (prop.isMiddleTableDefinition()) {
            middleTableOperator = new MiddleTableOperator(
                    ctx.prop(prop),
                    ctx.options.getDeleteMode() == DeleteMode.LOGICAL
            );
        } else {
            ImmutableProp mappedBy = prop.getMappedBy();
            if (mappedBy != null) {
                if (mappedBy.isColumnDefinition()) {
                    subOperator = new ChildTableOperator(
                            new DeleteContext(
                                    DeleteOptions.detach(ctx.options),
                                    ctx.con,
                                    ctx.trigger,
                                    ctx.affectedRowCountMap,
                                    ctx.path.to(prop)
                            )
                    );
                } else if (mappedBy.isMiddleTableDefinition()) {
                    middleTableOperator = new MiddleTableOperator(
                            ctx.prop(prop),
                            ctx.options.getDeleteMode() == DeleteMode.LOGICAL
                    );
                }
            }
        }
        if (subOperator == null && middleTableOperator == null) {
            return;
        }
        IdPairs.Retain noTargetIdPairs = new NoTargetEntityIdPairsImpl(rows);
        if (subOperator != null) {
            subOperator.disconnectExcept(noTargetIdPairs);
        }
        if (middleTableOperator != null) {
            middleTableOperator.disconnectExcept(noTargetIdPairs);
        }
    }

    private void updateAssociations(Batch<DraftSpi> batch, ImmutableProp prop, boolean detach) {
        ChildTableOperator subOperator = null;
        MiddleTableOperator middleTableOperator = null;
        if (prop.isMiddleTableDefinition()) {
            middleTableOperator = new MiddleTableOperator(
                    ctx.prop(prop),
                    ctx.options.getDeleteMode() == DeleteMode.LOGICAL
            );
        } else {
            ImmutableProp mappedBy = prop.getMappedBy();
            if (mappedBy != null) {
                if (mappedBy.isColumnDefinition()) {
                    subOperator = new ChildTableOperator(
                            new DeleteContext(
                                    DeleteOptions.detach(ctx.options),
                                    ctx.con,
                                    ctx.trigger,
                                    ctx.affectedRowCountMap,
                                    ctx.path.to(prop)
                            )
                    );
                } else if (mappedBy.isMiddleTableDefinition()) {
                    middleTableOperator = new MiddleTableOperator(
                            ctx.prop(prop),
                            ctx.options.getDeleteMode() == DeleteMode.LOGICAL
                    );
                }
            }
        }
        if (subOperator == null && middleTableOperator == null) {
            return;
        }
        IdPairs.Retain retainedIdPairs = IdPairs.retain(batch.entities(), prop);
        if (subOperator != null && detach && ctx.options.getAssociatedMode(prop) == AssociatedSaveMode.REPLACE) {
            subOperator.disconnectExcept(retainedIdPairs);
        }
        if (middleTableOperator != null) {
            if (detach) {
                switch (ctx.options.getAssociatedMode(prop)) {
                    case APPEND:
                    case APPEND_IF_ABSENT:
                    case VIOLENTLY_REPLACE:
                        middleTableOperator.append(retainedIdPairs);
                        break;
                    case UPDATE:
                    case MERGE:
                        middleTableOperator.merge(retainedIdPairs);
                        break;
                    case REPLACE:
                        middleTableOperator.replace(retainedIdPairs);
                        break;
                }
            } else {
                middleTableOperator.append(retainedIdPairs);
            }
        }
    }

    private boolean isReadOnlyMiddleTable(ImmutableProp prop) {
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null) {
            prop = mappedBy;
        }
        if (prop.isMiddleTableDefinition()) {
            MiddleTable middleTable = prop.getStorage(ctx.options.getSqlClient().getMetadataStrategy());
            return middleTable.isReadonly();
        }
        return false;
    }
}
