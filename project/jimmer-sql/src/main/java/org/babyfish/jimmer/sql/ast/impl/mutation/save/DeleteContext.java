package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteOptions;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.runtime.MutationPath;

import java.sql.Connection;
import java.util.Map;

class DeleteContext {

    final DeleteOptions options;

    final Connection con;

    final MutationTrigger trigger;

    final boolean triggerSubmitImmediately;

    final Map<AffectedTable, Integer> affectedRowCountMap;

    final MutationPath path;

    DeleteContext(
            DeleteOptions options,
            Connection con,
            MutationTrigger trigger,
            boolean triggerSubmitImmediately,
            Map<AffectedTable, Integer> affectedRowCountMap,
            MutationPath path
    ) {
        this.options = options;
        this.con = con;
        this.trigger = trigger;
        this.triggerSubmitImmediately = triggerSubmitImmediately;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = path;
    }
}
