package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteOptions;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.runtime.MutationPath;

import java.sql.Connection;
import java.util.Map;

class DeleteContext {

    final DeleteContext parent;

    final DeleteOptions options;

    final Connection con;

    final MutationTrigger trigger;

    final boolean triggerSubmitImmediately;

    final Map<AffectedTable, Integer> affectedRowCountMap;

    final MutationPath path;

    final ImmutableProp backReferenceProp;

    DeleteContext(
            DeleteOptions options,
            Connection con,
            MutationTrigger trigger,
            boolean triggerSubmitImmediately,
            Map<AffectedTable, Integer> affectedRowCountMap,
            MutationPath path
    ) {
        ImmutableProp mappedBy = path.getProp() != null ? path.getProp().getMappedBy() : null;
        if (mappedBy != null && !mappedBy.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "The property \"" +
                            path.getProp() +
                            "\" does not reference child table"
            );
        }
        this.parent = null;
        this.options = options;
        this.con = con;
        this.trigger = trigger;
        this.triggerSubmitImmediately = triggerSubmitImmediately;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = path;
        this.backReferenceProp = mappedBy;
    }

    private DeleteContext(DeleteContext parent, ImmutableProp backReferenceProp) {
        if (!backReferenceProp.isReference(TargetLevel.ENTITY) || !backReferenceProp.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "The back reference property \"" +
                            backReferenceProp +
                            "\" is not reference association with column definition"
            );
        }
        this.parent = parent;
        this.options = parent.options;
        this.con = parent.con;
        this.trigger = parent.trigger;
        this.triggerSubmitImmediately = parent.triggerSubmitImmediately;
        this.affectedRowCountMap = parent.affectedRowCountMap;
        this.path = parent.path.backReferenceOf(backReferenceProp);
        this.backReferenceProp = backReferenceProp;
    }

    DeleteContext backReferenceOf(ImmutableProp backReferenceProp) {
        return new DeleteContext(this, backReferenceProp);
    }
}
