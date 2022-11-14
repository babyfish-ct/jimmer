package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;

import java.util.ArrayList;
import java.util.List;

class MutationTrigger {

    private final List<ChangedObject> changedObjects = new ArrayList<>();

    public void prepare(Object oldEntity, Object newEntity) {
        changedObjects.add(new ChangedEntity(oldEntity, newEntity));
    }

    public void prepare(ImmutableProp prop, Object sourceId, Object detachedTargetId, Object attachedTargetId) {
        changedObjects.add(new ChangedAssociation(prop, sourceId, detachedTargetId, attachedTargetId));
    }

    private interface ChangedObject {}

    private static class ChangedEntity implements ChangedObject {

        final Object oldEntity;

        final Object newEntity;

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

    private static class ChangedAssociation implements ChangedObject {

        final ImmutableProp prop;

        final Object sourceId;

        final Object detachedTargetId;

        final Object attachedTargetId;

        private ChangedAssociation(ImmutableProp prop, Object sourceId, Object detachedTargetId, Object attachedTargetId) {
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
