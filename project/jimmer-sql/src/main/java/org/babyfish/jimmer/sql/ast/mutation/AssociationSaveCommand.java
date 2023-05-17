package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.jetbrains.annotations.Nullable;

public interface AssociationSaveCommand extends Executable<Integer> {

    @NewChain
    AssociationSaveCommand checkExistence(@Nullable Boolean checkExistence);
}
