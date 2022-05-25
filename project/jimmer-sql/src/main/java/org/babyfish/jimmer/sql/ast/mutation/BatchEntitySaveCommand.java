package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.Executable;

public interface BatchEntitySaveCommand<E>
        extends Executable<BatchSaveResult<E>>,
        AbstractEntitySaveCommand<BatchEntitySaveCommand<E>> {
}
