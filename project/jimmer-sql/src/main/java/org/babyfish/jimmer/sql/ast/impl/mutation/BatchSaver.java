package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.UnloadedException;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.babyfish.jimmer.sql.runtime.SavePath;

import java.sql.Connection;
import java.util.*;

public class BatchSaver {

    private static final String GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON = "Joining is disabled in general optimistic lock";

    private final ShapedEntityMap<ImmutableSpi> entityMap = new ShapedEntityMap<>();

    private final AbstractEntitySaveCommandImpl.Data data;

    private final Connection con;

    private final SaverCache cache;

    private final MutationTrigger trigger;

    private final boolean triggerSubmitImmediately;

    private final Map<AffectedTable, Integer> affectedRowCountMap;

    private final SavePath path;

    private boolean triggerSubmitted;

    BatchSaver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con,
            ImmutableType type
    ) {
        this(data, con, type, new SaverCache(data), true, new LinkedHashMap<>());
    }

    BatchSaver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con,
            ImmutableType type,
            SaverCache cache,
            boolean triggerSubmitImmediately,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        this.data = data;
        this.con = con;
        this.cache = cache;
        this.trigger = data.getTriggers() != null ? new MutationTrigger() : null;
        this.triggerSubmitImmediately = triggerSubmitImmediately && this.trigger != null;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = SavePath.root(type);
    }

    BatchSaver(BatchSaver base, AbstractEntitySaveCommandImpl.Data data, ImmutableProp prop) {
        this.data = data;
        this.con = base.con;
        this.cache = base.cache;
        this.trigger = base.trigger;
        this.triggerSubmitImmediately = this.trigger != null;
        this.affectedRowCountMap = base.affectedRowCountMap;
        this.path = base.path.to(prop);
    }

    @SuppressWarnings("unchecked")
    public <E> void add(E entity) {
        if (!(entity instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("The argument \"entity\" must be immutable object");
        }
        if (entity instanceof DraftSpi) {
            throw new IllegalArgumentException("The argument \"entity\" cannot be draft object");
        }
        entityMap.add((ImmutableSpi) entity);
    }

    @SuppressWarnings("unchecked")
    public boolean execute() {
        Batch<ImmutableSpi> batch = entityMap.remove();
        if (batch == null) {
            return false;
        }
        List<Object> newEntities = Internal.produceList(
                batch.shape().getType(),
                batch.entities(),
                drafts -> {
                    save(Batch.of(batch.shape(), (List<DraftSpi>) drafts));
                },
                trigger == null ? null : trigger::prepareSubmit
        );
        if (triggerSubmitImmediately) {
            assert trigger != null;
            trigger.submit(data.getSqlClient(), con);
        }
        return true;
    }

    private void save(Batch<DraftSpi> batch) {
        SaveShape shape = batch.shape();
        if (!shape.isIdLoaded() && !shape.isAllKeysLoaded()) {
            throw new SaveException.NeitherIdNorKey(
                    path,
                    "Cannot save illegal entity object " +
                            batch.entities().get(0) +
                            " whose type is \"" +
                            shape.getType() +
                            "\", key properties " +
                            shape.getType().getKeyProps() +
                            " must be loaded when id is unloaded"
            );
        }
        saveAssociations(batch, true);
        saveAssociations(batch, false);
    }

    @SuppressWarnings("unchecked")
    private void saveAssociations(Batch<DraftSpi> batch, boolean forParent) {
        ImmutableType currentType = batch.shape().getType();
        PropId currentIdPropId = currentType.getIdProp().getId();
        for (ImmutableProp prop : batch.shape().getProps()) {
            if (prop.isAssociation(TargetLevel.ENTITY) && prop.isColumnDefinition() == forParent) {
                if (isReadOnlyMiddleTable(prop)) {
                    throw new SaveException.ReadonlyMiddleTable(
                            path,
                            "The property \"" +
                                    prop +
                                    "\" which is based on readonly middle table cannot be saved"
                    );
                }
                ImmutableType targetType = prop.getTargetType();
                if (prop.isRemote() && prop.getMappedBy() != null) {
                    throw new SaveException.ReversedRemoteAssociation(
                            path,
                            "The property \"" +
                                    prop +
                                    "\" which is reversed(with `mappedBy`) remote(across different microservices) association " +
                                    "cannot be supported by save command"
                    );
                }
                if (prop.getSqlTemplate() instanceof JoinTemplate) {
                    throw new SaveException.UnstructuredAssociation(
                            path,
                            "The property \"" +
                                    prop +
                                    "\" which is unstructured association(decorated by @" +
                                    JoinSql.class.getName() +
                                    ") " +
                                    "cannot be supported by save command"
                    );
                }
                ImmutableProp mappedBy = prop.getMappedBy();
                ChildTableOperator childTableOperator = null;
                if (!prop.isRemote() && mappedBy != null && mappedBy.isColumnDefinition()) {
                    childTableOperator = new ChildTableOperator(
                            data.getSqlClient(),
                            con,
                            mappedBy,
                            data.getLockMode() == LockMode.PESSIMISTIC,
                            cache,
                            trigger
                    );
                }

                Set<Object> associatedObjectIds = new LinkedHashSet<>();
                List<Object> idOnlyTargetIds = new ArrayList<>();
                Map<Object, Collection<Object>> idOnlyTargetIdMap = new LinkedHashMap<>();
                List<DraftSpi> savableTargets = new ArrayList<>();
                Map<Object, Collection<DraftSpi>> savableTargetMap = new LinkedHashMap<>();
                for (DraftSpi currentDraftSpi : batch.entities()) {
                    Object currentId = currentDraftSpi.__isLoaded(currentIdPropId) ?
                            currentDraftSpi.__get(currentIdPropId) :
                            null;
                    Object associatedValue = currentDraftSpi.__get(prop.getId());
                    if (associatedValue == null) {
                        if (prop.isInputNotNull()) {
                            throw new SaveException.NullTarget(
                                    path,
                                    "The association \"" +
                                            prop +
                                            "\" cannot be null, because that association is decorated by \"@" +
                                            (
                                                    prop.getAnnotation(ManyToOne.class) != null ?
                                                            ManyToOne.class :
                                                            OneToOne.class
                                            ).getName() +
                                            "\" whose `inputNotNull` is true"
                            );
                        }
                    } else {
                        List<DraftSpi> associatedObjects =
                                associatedValue instanceof List<?> ?
                                        (List<DraftSpi>) associatedValue :
                                        Collections.singletonList((DraftSpi) associatedValue);
                        if (data.isAutoCheckingProp(prop) || childTableOperator != null) {
                            PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
                            for (DraftSpi associatedObject : associatedObjects) {
                                if (!isNonIdPropLoaded(associatedObject)) {
                                    Object targetId;
                                    try {
                                        targetId = associatedObject.__get(targetIdPropId);
                                    } catch (UnloadedException ex) {
                                        throw new SaveException.EmptyObject(
                                                path,
                                                "An associated object of the property \"" +
                                                        prop +
                                                        "\" does not have any properties"
                                        );
                                    }
                                    idOnlyTargetIds.add(targetId);
                                    idOnlyTargetIdMap
                                            .computeIfAbsent(currentId, it -> new ArrayList<>())
                                            .add(targetId);
                                } else if (prop.isRemote()) {
                                    throw new SaveException.LongRemoteAssociation(
                                            path,
                                            "The property \"" +
                                                    prop +
                                                    "\" is remote(across different microservices) association, " +
                                                    "but it has associated object which is not id-only"
                                    );
                                } else {
                                    savableTargets.addAll(associatedObjects);
                                    savableTargetMap
                                            .computeIfAbsent(currentId, it -> new ArrayList<>())
                                            .addAll(associatedObjects);
                                }
                            }
                        }
                    }
                }
                if (data.isAutoCheckingProp(prop)) {
                    validateIdOnlyTargetIds(prop, idOnlyTargetIds);
                }
                saveAssociatedObjectsAndGetIds(savableTargets);
                if (childTableOperator != null) {
                    for (Map.Entry<Object, Collection<DraftSpi>> e : savableTargetMap.entrySet()) {
                        Object currentId = e.getKey();
                        for (DraftSpi draft : e.getValue()) {
                            draft.__set(
                                    mappedBy.getId(),
                                    Internal.produce(currentType, null, backRef -> {
                                        ((DraftSpi) backRef).__set(currentIdPropId, currentId);
                                    })
                            );
                        }
                    }
                    if (!idOnlyTargetIdMap.isEmpty()) {
                        int rowCount = 0; // TODO: childTableOperator.setParent(currentId, idOnlyTargetIds);
                        addOutput(AffectedTable.of(targetType), rowCount);
                    }
                }
//                if (childTableOperator != null &&
//                        currentObjectType != Saver.ObjectType.NEW &&
//                        data.getAssociatedMode(prop) == AssociatedSaveMode.REPLACE
//                ) {
//                    DissociateAction dissociateAction = data.getDissociateAction(prop.getMappedBy());
//                    if (dissociateAction == DissociateAction.DELETE) {
//                        List<Object> detachedTargetIds = childTableOperator.getDetachedChildIds(
//                                currentId,
//                                associatedObjectIds
//                        );
//                        Deleter deleter = new Deleter(
//                                new DeleteCommandImpl.Data(
//                                        data.getSqlClient(),
//                                        data.getDeleteMode(),
//                                        data.dissociateActionMap()
//                                ),
//                                con,
//                                cache,
//                                trigger,
//                                affectedRowCountMap
//                        );
//                        deleter.addPreHandleInput(prop.getTargetType(), detachedTargetIds);
//                        deleter.execute(false);
//                    } else if (dissociateAction == DissociateAction.SET_NULL) {
//                        int rowCount = childTableOperator.unsetParent(currentId, associatedObjectIds);
//                        addOutput(AffectedTable.of(targetType), rowCount);
//                    } else {
//                        if (childTableOperator.exists(currentId, associatedObjectIds)) {
//                            throw new SaveException.CannotDissociateTarget(
//                                    path.to(prop),
//                                    "Cannot dissociate child objects because the dissociation action of the many-to-one property \"" +
//                                            mappedBy +
//                                            "\" is not configured as \"set null\" or \"cascade\". " +
//                                            "There are two ways to resolve this issue: Decorate the many-to-one property \"" +
//                                            mappedBy +
//                                            "\" by @" +
//                                            OnDissociate.class.getName() +
//                                            " whose argument is `DissociateAction.SET_NULL` or `DissociateAction.DELETE` " +
//                                            ", or use save command's runtime configuration to override it"
//                            );
//                        }
//                    }
//                }
//                MiddleTableOperator middleTableOperator = MiddleTableOperator.tryGet(
//                        data.getSqlClient(), con, prop, trigger
//                );
//                if (middleTableOperator != null) {
//                    int rowCount;
//                    AssociatedSaveMode associatedMode = data.getAssociatedMode(prop);
//                    if (currentObjectType == Saver.ObjectType.NEW || associatedMode == AssociatedSaveMode.APPEND) {
//                        rowCount = middleTableOperator.addTargetIds(
//                                currentId,
//                                associatedObjectIds
//                        );
//                    } else if (associatedMode == AssociatedSaveMode.MERGE) {
//                        middleTableOperator.getTargetIds(currentId).forEach(associatedObjectIds::remove);
//                        rowCount = middleTableOperator.addTargetIds(currentId, associatedObjectIds);
//                    } else {
//                        rowCount = middleTableOperator.setTargetIds(
//                                currentId,
//                                associatedObjectIds
//                        );
//                    }
//                    addOutput(AffectedTable.of(prop), rowCount);
//                }
            }
        }
    }

    private boolean isReadOnlyMiddleTable(ImmutableProp prop) {
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null) {
            prop = mappedBy;
        }
        if (prop.isMiddleTableDefinition()) {
            MiddleTable middleTable = prop.getStorage(data.getSqlClient().getMetadataStrategy());
            return middleTable.isReadonly();
        }
        return false;
    }

    private boolean isNonIdPropLoaded(ImmutableSpi spi) {
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (spi.__isLoaded(prop.getId())) {
                if (!prop.isId()) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void validateIdOnlyTargetIds(ImmutableProp prop, List<Object> targetIds) {
        if (targetIds.isEmpty()) {
            return;
        }
        Set<Object> illegalTargetIds = new LinkedHashSet<>(targetIds.size());
        for (Object targetId : targetIds) {
            if (!cache.hasId(prop.getTargetType(), targetId)) {
                illegalTargetIds.add(targetId);
            }
        }
        if (illegalTargetIds.isEmpty()) {
            return;
        }
        if (prop.isRemote()) {
            PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
            List<ImmutableSpi> targets;
            try {
                targets = data
                        .getSqlClient()
                        .getMicroServiceExchange()
                        .findByIds(
                                prop.getTargetType().getMicroServiceName(),
                                illegalTargetIds,
                                new FetcherImpl<>((Class<ImmutableSpi>) (prop.getTargetType().getJavaClass()))
                        );
            } catch (Exception ex) {
                throw new SaveException.FailedRemoteValidation(
                        path,
                        "Cannot validate the id-only associated objects of remote association \"" +
                                prop +
                                "\""
                );
            }
            for (ImmutableSpi target : targets) {
                illegalTargetIds.remove(target.__get(targetIdPropId));
            }
        } else {
            List<Object> existingTargetIds = Queries
                    .createQuery(
                            data.getSqlClient(),
                            prop.getTargetType(),
                            ExecutionPurpose.MUTATE,
                            FilterLevel.DEFAULT,
                            (q, t) -> {
                                Expression<Object> idExpr = t.get(prop.getTargetType().getIdProp());
                                q.where(idExpr.in(illegalTargetIds));
                                return q.select(idExpr);
                            }
                    ).execute(con);
            illegalTargetIds.removeAll(new HashSet<>(existingTargetIds));
        }
        if (!illegalTargetIds.isEmpty()) {
            throw new SaveException.IllegalTargetId(
                    path.to(prop),
                    "Illegal ids: " + illegalTargetIds
            );
        }
    }

    private void addOutput(AffectedTable affectTable, int affectedRowCount) {
        if (affectedRowCount != 0) {
            affectedRowCountMap.merge(affectTable, affectedRowCount, Integer::sum);
        }
    }

    private List<Object> saveTargetsAndGetIds(ImmutableProp prop, Collection<DraftSpi> drafts) {
        AbstractEntitySaveCommandImpl.Data associatedData =
                new AbstractEntitySaveCommandImpl.Data(data);
        associatedData.setMode(
                data.getAssociatedMode(prop) == AssociatedSaveMode.APPEND ?
                        SaveMode.INSERT_ONLY :
                        SaveMode.UPSERT
        );
        BatchSaver associatedSaver = new BatchSaver(this, associatedData, prop);
        for (DraftSpi draft : drafts) {
            associatedSaver.add(draft);
        }
        while (associatedSaver.execute());
        List<Object> targetIds = new ArrayList<>(drafts.size());
        PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
        for (DraftSpi draft : drafts) {
            targetIds.add(draft.__get(targetIdPropId));
        }
        return targetIds;
    }

    private ObjectType saveSelf(Batch<DraftSpi> drafts) {

    }

    private enum ObjectType {
        UNKNOWN,
        NEW,
        EXISTING
    }
}
