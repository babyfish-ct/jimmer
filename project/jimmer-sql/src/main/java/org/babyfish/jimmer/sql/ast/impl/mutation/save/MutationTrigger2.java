package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.Triggers;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class MutationTrigger2 {

    private final List<MutationTrigger2.ChangedData> changedList = new ArrayList<>();

    public void modifyEntityTable(Object oldEntity, Object newEntity) {
        changedList.add(new MutationTrigger2.EntityChangedData(oldEntity, newEntity));
    }

    public void insertMiddleTable(ImmutableProp prop, Object sourceId, Object targetId) {
        changedList.add(new MutationTrigger2.AssociationChangedData(prop, sourceId, null, targetId));
    }

    public void deleteMiddleTable(ImmutableProp prop, Object sourceId, Object targetId) {
        changedList.add(new MutationTrigger2.AssociationChangedData(prop, sourceId, targetId, null));
    }

    public void prepareSubmit(DraftContext ctx) {
        if (!changedList.isEmpty()) {
            for (MutationTrigger2.ChangedData changedData : this.changedList) {
                if (changedData instanceof MutationTrigger2.EntityChangedData) {
                    MutationTrigger2.EntityChangedData data = (MutationTrigger2.EntityChangedData) changedData;
                    data.newEntity = ctx.resolveObject(data.newEntity);
                }
                if (changedData instanceof MutationTrigger2.AssociationChangedData) {
                    MutationTrigger2.AssociationChangedData data = (MutationTrigger2.AssociationChangedData) changedData;
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
            for (MutationTrigger2.ChangedData changedData : this.changedList) {
                if (changedData instanceof MutationTrigger2.EntityChangedData) {
                    MutationTrigger2.EntityChangedData data = (MutationTrigger2.EntityChangedData) changedData;
                    triggers.fireEntityTableChange(data.oldEntity, ImmutableObjects.toLonely(data.newEntity), con);
                } else {
                    MutationTrigger2.AssociationChangedData data = (MutationTrigger2.AssociationChangedData) changedData;
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

    private static class EntityChangedData implements MutationTrigger2.ChangedData {

        final Object oldEntity;

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

    private static class AssociationChangedData implements MutationTrigger2.ChangedData {

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
}

