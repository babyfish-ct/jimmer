package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

public interface DeleteOptions {

    JSqlClientImplementor getSqlClient();

    DeleteMode getMode();

    DissociateAction getDissociateAction(ImmutableProp backReferenceProp);
}
