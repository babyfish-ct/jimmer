package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.Triggers;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

class MutationTrigger {

    private final List<ChangedData> changedList = new ArrayList<>();

    public void modifyEntityTable(Object oldEntity, Object newEntity) {
        changedList.add(new EntityChangedData(oldEntity, newEntity));
    }

    public void insertMiddleTable(ImmutableProp prop, Object sourceId, Object targetId) {
        changedList.add(new AssociationChangedData(prop, sourceId, null, targetId));
    }

    public void deleteMiddleTable(ImmutableProp prop, Object sourceId, Object targetId) {
        changedList.add(new AssociationChangedData(prop, sourceId, targetId, null));
    }

    public void prepareSubmit(DraftContext ctx) {
        if (!changedList.isEmpty()) {
            for (ChangedData changedData : this.changedList) {
                if (changedData instanceof EntityChangedData) {
                    EntityChangedData data = (EntityChangedData) changedData;
                    data.newEntity = ctx.resolveObject(data.newEntity);
                }
                if (changedData instanceof AssociationChangedData) {
                    AssociationChangedData data = (AssociationChangedData) changedData;
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
            for (ChangedData changedData : this.changedList) {
                if (changedData instanceof EntityChangedData) {
                    EntityChangedData data = (EntityChangedData) changedData;
                    triggers.fireEntityTableChange(data.oldEntity, ImmutableObjects.toLonely(data.newEntity), con);
                } else {
                    AssociationChangedData data = (AssociationChangedData) changedData;
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

    private static class EntityChangedData implements ChangedData {

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

    private static class AssociationChangedData implements ChangedData {

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
