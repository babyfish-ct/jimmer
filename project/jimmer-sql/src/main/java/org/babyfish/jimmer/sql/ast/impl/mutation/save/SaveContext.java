package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.OneToMany;
import org.babyfish.jimmer.sql.ast.impl.mutation.SaveOptions;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.runtime.SavePath;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

class SaveContext {

    final SaveOptions options;

    final Connection con;

    final MutationTrigger trigger;

    final boolean triggerSubmitImmediately;

    final Map<AffectedTable, Integer> affectedRowCountMap;

    final SavePath path;

    final ImmutableProp backReferenceProp;

    final boolean backReferenceFrozen;

    private boolean triggerSubmitted;

    SaveContext(
            SaveOptions options,
            Connection con,
            ImmutableType type
    ) {
        this(options, con, type, true, new LinkedHashMap<>());
    }

    SaveContext(
            SaveOptions options,
            Connection con,
            ImmutableType type,
            boolean triggerSubmitImmediately,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        this.options = options;
        this.con = con;
        this.trigger = options.getTriggers() != null ? new MutationTrigger() : null;
        this.triggerSubmitImmediately = triggerSubmitImmediately && this.trigger != null;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = SavePath.root(type);
        this.backReferenceProp = null;
        this.backReferenceFrozen = false;
    }

    SaveContext(SaveContext base, SaveOptions options, ImmutableProp prop) {
        this.options = options;
        this.con = base.con;
        this.trigger = base.trigger;
        this.triggerSubmitImmediately = this.trigger != null;
        this.affectedRowCountMap = base.affectedRowCountMap;
        this.path = base.path.to(prop);
        if (prop.getAssociationAnnotation().annotationType() == OneToMany.class) {
            this.backReferenceProp = prop.getMappedBy();
            this.backReferenceFrozen = !((OneToMany)prop.getAssociationAnnotation()).isTargetTransferable();
        } else {
            this.backReferenceProp = null;
            this.backReferenceFrozen = false;
        }
    }
}
