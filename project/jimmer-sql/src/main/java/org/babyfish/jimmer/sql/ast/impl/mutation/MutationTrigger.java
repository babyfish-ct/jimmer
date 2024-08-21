package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.Triggers;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class MutationTrigger {

    private final List<MutationTrigger.ChangedData> changedList = new ArrayList<>();

    public void modifyEntityTable(Object oldEntity, Object newEntity) {
        changedList.add(new MutationTrigger.EntityChangedData(oldEntity, newEntity));
    }

    public void insertMiddleTable(ImmutableProp prop, Object sourceId, Object targetId) {
        changedList.add(new MutationTrigger.AssociationChangedData(prop, sourceId, null, targetId));
    }

    public void deleteMiddleTable(ImmutableProp prop, Object sourceId, Object targetId) {
        changedList.add(new MutationTrigger.AssociationChangedData(prop, sourceId, targetId, null));
    }

    public void prepareSubmit(DraftContext ctx) {
        if (!changedList.isEmpty()) {
            for (MutationTrigger.ChangedData changedData : this.changedList) {
                if (changedData instanceof MutationTrigger.EntityChangedData) {
                    MutationTrigger.EntityChangedData data = (MutationTrigger.EntityChangedData) changedData;
                    data.oldEntity = ctx.resolveObject(data.oldEntity);
                    data.newEntity = ctx.resolveObject(data.newEntity);
                }
                if (changedData instanceof MutationTrigger.AssociationChangedData) {
                    MutationTrigger.AssociationChangedData data = (MutationTrigger.AssociationChangedData) changedData;
                    data.sourceId = ctx.resolveObject(data.sourceId);
                    data.detachedTargetId = ctx.resolveObject(data.detachedTargetId);
                    data.attachedTargetId = ctx.resolveObject(data.attachedTargetId);
                }
            }
        }
    }

    public void submit(JSqlClient sqlClient, Connection con) {
        if (!changedList.isEmpty()) {
            Triggers triggers = sqlClient.getTriggers(true);
            for (MutationTrigger.ChangedData changedData : this.changedList) {
                if (changedData instanceof MutationTrigger.EntityChangedData) {
                    MutationTrigger.EntityChangedData data = (MutationTrigger.EntityChangedData) changedData;
                    Internal.requiresNewDraftContext(ctx -> {
                        triggers.fireEntityTableChange(
                                toLonely((ImmutableSpi) data.oldEntity),
                                toLonely((ImmutableSpi) data.newEntity),
                                con
                        );
                        return null;
                    });
                } else {
                    MutationTrigger.AssociationChangedData data = (MutationTrigger.AssociationChangedData) changedData;
                    if (data.detachedTargetId == null) {
                        triggers.fireMiddleTableInsert(data.prop, data.sourceId, data.attachedTargetId, con);
                    } else {
                        triggers.fireMiddleTableDelete(data.prop, data.sourceId, data.detachedTargetId, con);
                    }
                }
            }
        }
    }

    private interface ChangedData {}

    private static class EntityChangedData implements MutationTrigger.ChangedData {

        Object oldEntity;

        Object newEntity;

        private EntityChangedData(Object oldEntity, Object newEntity) {
            this.oldEntity = oldEntity;
            this.newEntity = newEntity;
        }

        @Override
        public String toString() {
            return "ChangedEntity{" +
                    "oldEntity=" + oldEntity +
                    ", newEntity=" + newEntity +
                    '}';
        }
    }

    private static class AssociationChangedData implements MutationTrigger.ChangedData {

        final ImmutableProp prop;

        Object sourceId;

        Object detachedTargetId;

        Object attachedTargetId;

        private AssociationChangedData(ImmutableProp prop, Object sourceId, Object detachedTargetId, Object attachedTargetId) {
            this.prop = prop;
            this.sourceId = sourceId;
            this.detachedTargetId = detachedTargetId;
            this.attachedTargetId = attachedTargetId;
        }

        @Override
        public String toString() {
            return "ChangedAssociation{" +
                    "prop=" + prop +
                    ", sourceId=" + sourceId +
                    ", detachedTargetId=" + detachedTargetId +
                    ", attachedTargetId=" + attachedTargetId +
                    '}';
        }
    }

    private static ImmutableSpi toLonely(ImmutableSpi spi) {
        if (spi == null) {
            return null;
        }
        ImmutableType type = spi.__type();
        return Internal.requiresNewDraftContext(ctx -> {
            DraftSpi draft = (DraftSpi) type.getDraftFactory().apply(ctx, null);
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isColumnDefinition()) {
                    PropId propId = prop.getId();
                    if (spi.__isLoaded(propId)) {
                        if (prop.isReference(TargetLevel.ENTITY)) {
                            draft.__set(propId, toIdOnly((ImmutableSpi) spi.__get(propId)));
                        } else if (prop.isReference(TargetLevel.OBJECT)) {
                            draft.__set(propId, toLonely((ImmutableSpi) spi.__get(propId)));
                        } else {
                            draft.__set(propId, spi.__get(propId));
                        }
                    }
                }
            }
            return ctx.resolveObject((ImmutableSpi) draft);
        });
    }

    private static ImmutableSpi toIdOnly(ImmutableSpi spi) {
        if (spi == null) {
            return null;
        }
        PropId idPropId = spi.__type().getIdProp().getId();
        return ImmutableObjects.makeIdOnly(spi.__type(), spi.__get(idPropId));
    }
}

