package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Executable;

public interface AssociationSaveCommand extends Executable<Integer> {

    @NewChain
    AssociationSaveCommand checkExistence(Boolean checkExistence);
}
