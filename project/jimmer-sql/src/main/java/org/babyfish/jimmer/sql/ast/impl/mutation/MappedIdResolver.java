package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.MappedId;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.List;
import java.util.Objects;

class MappedIdResolver {

    private final SaveContext ctx;

    MappedIdResolver(SaveContext ctx) {
        this.ctx = ctx;
    }

    void resolve(List<DraftSpi> drafts) {
        List<MappedId> mappedIds = ctx.path.getType().getMappedIds();
        if (mappedIds.isEmpty()) {
            return;
        }
        for (DraftSpi draft : drafts) {
            for (MappedId mappedId : mappedIds) {
                resolve(draft, mappedId);
            }
        }
    }

    private void resolve(DraftSpi draft, MappedId mappedId) {
        ImmutableProp prop = mappedId.getProp();
        PropId propId = prop.getId();
        if (!draft.__isLoaded(propId)) {
            return;
        }
        Object target = draft.__get(propId);
        if (target == null) {
            return;
        }
        ImmutableSpi targetSpi = (ImmutableSpi) target;
        ImmutableProp targetIdProp = mappedId.getTargetIdProp();
        PropId targetIdPropId = targetIdProp.getId();
        if (!targetSpi.__isLoaded(targetIdPropId)) {
            throw ctx.prop(prop).createInconsistentMappedId(
                    mappedId,
                    "the target id property \"" +
                            targetIdProp +
                            "\" is not loaded"
            );
        }
        Object targetId = targetSpi.__get(targetIdPropId);
        if (mappedId.isFull()) {
            setFullId(draft, mappedId, targetId);
        } else {
            setPartialId(draft, mappedId, targetId);
        }
    }

    private void setFullId(DraftSpi draft, MappedId mappedId, Object targetId) {
        ImmutableProp idProp = mappedId.getIdProp();
        PropId idPropId = idProp.getId();
        if (draft.__isLoaded(idPropId)) {
            Object id = draft.__get(idPropId);
            if (id != null && !Objects.equals(id, targetId)) {
                throw ctx.createInconsistentMappedId(
                        mappedId,
                        "the id value \"" +
                                id +
                                "\" does not match the target id value \"" +
                                targetId +
                                "\""
                );
            }
        }
        draft.__set(idPropId, targetId);
    }

    private void setPartialId(DraftSpi draft, MappedId mappedId, Object targetId) {
        ImmutableProp idProp = mappedId.getIdProp();
        PropId idPropId = idProp.getId();
        DraftSpi idDraft;
        if (draft.__isLoaded(idPropId)) {
            Object id = draft.__get(idPropId);
            idDraft = id != null ? toDraft(draft, id) : newDraft(draft, idProp.getTargetType());
        } else {
            idDraft = newDraft(draft, idProp.getTargetType());
        }
        setPath(idDraft, mappedId, targetId);
        draft.__set(idPropId, idDraft);
    }

    private void setPath(DraftSpi parent, MappedId mappedId, Object targetId) {
        List<ImmutableProp> path = mappedId.getIdPath();
        for (int i = 0; i < path.size() - 1; i++) {
            ImmutableProp prop = path.get(i);
            PropId propId = prop.getId();
            DraftSpi child;
            if (parent.__isLoaded(propId)) {
                Object value = parent.__get(propId);
                child = value != null ? toDraft(parent, value) : newDraft(parent, prop.getTargetType());
            } else {
                child = newDraft(parent, prop.getTargetType());
            }
            parent.__set(propId, child);
            parent = child;
        }
        ImmutableProp leafProp = path.get(path.size() - 1);
        PropId leafPropId = leafProp.getId();
        if (parent.__isLoaded(leafPropId)) {
            Object value = parent.__get(leafPropId);
            if (value != null && !Objects.equals(value, targetId)) {
                throw ctx.createInconsistentMappedId(
                        mappedId,
                        "the id path value \"" +
                                value +
                                "\" does not match the target id value \"" +
                                targetId +
                                "\""
                );
            }
        }
        parent.__set(leafPropId, targetId);
    }

    private DraftSpi toDraft(DraftSpi owner, Object value) {
        return owner.__draftContext().toDraftObject(value);
    }

    private DraftSpi newDraft(DraftSpi owner, ImmutableType type) {
        return (DraftSpi) type.getDraftFactory().apply(
                owner.__draftContext(),
                null
        );
    }
}
