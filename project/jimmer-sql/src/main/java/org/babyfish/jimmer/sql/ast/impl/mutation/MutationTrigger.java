package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.Triggers;

import java.util.ArrayList;
import java.util.List;

class MutationTrigger {

    private final List<ChangedObject> changedObjects = new ArrayList<>();

    public void modifyEntityTable(Object oldEntity, Object newEntity) {
        changedObjects.add(new ChangedEntity(oldEntity, newEntity));
    }

    public void insertMiddleTable(ImmutableProp prop, Object sourceId, Object targetId) {
        changedObjects.add(new ChangedMiddleData(prop, sourceId, null, targetId));
    }

    public void deleteMiddleTable(ImmutableProp prop, Object sourceId, Object targetId) {
        changedObjects.add(new ChangedMiddleData(prop, sourceId, targetId, null));
    }

    public void prepareSubmit(DraftContext ctx) {
        if (!changedObjects.isEmpty()) {
            for (ChangedObject changedObject : changedObjects) {
                if (changedObject instanceof ChangedEntity) {
                    ChangedEntity entity = (ChangedEntity) changedObject;
                    entity.newEntity = ctx.resolveObject(entity.newEntity);
                }
            }
        }
    }

    public void submit(JSqlClient sqlClient) {
        if (!changedObjects.isEmpty()) {
            Triggers triggers = sqlClient.getTriggers(true);
            for (ChangedObject changedObject : changedObjects) {
                if (changedObject instanceof ChangedEntity) {
                    ChangedEntity entity = (ChangedEntity) changedObject;
                    triggers.fireEntityTableChange(entity.oldEntity, ImmutableObjects.toLonely(entity.newEntity));
                } else {
                    ChangedMiddleData association = (ChangedMiddleData) changedObject;
                    if (association.detachedTargetId == null) {
                        triggers.fireMiddleTableInsert(association.prop, association.sourceId, association.attachedTargetId);
                    } else {
                        triggers.fireMiddleTableDelete(association.prop, association.sourceId, association.detachedTargetId);
                    }
                }
            }
        }
    }

    private interface ChangedObject {}

    private static class ChangedEntity implements ChangedObject {

        final Object oldEntity;

        Object newEntity;

        private ChangedEntity(Object oldEntity, Object newEntity) {
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

    private static class ChangedMiddleData implements ChangedObject {

        final ImmutableProp prop;

        final Object sourceId;

        final Object detachedTargetId;

        final Object attachedTargetId;

        private ChangedMiddleData(ImmutableProp prop, Object sourceId, Object detachedTargetId, Object attachedTargetId) {
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
