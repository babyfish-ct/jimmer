package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.Executable;

public interface SimpleSaveCommand<E>
        extends Executable<SimpleSaveResult<E>>,
        AbstractSaveCommand<SimpleSaveCommand<E>> {

}
