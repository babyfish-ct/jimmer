package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteOptions;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.Map;

class DeleteOptionsImpl implements DeleteOptions {

    private final JSqlClientImplementor sqlClient;

    private final DeleteMode mode;

    private final DissociateAction defaultDissociateAction;

    private final Map<ImmutableProp, DissociateAction> dissociateActionMap;

    DeleteOptionsImpl(
            JSqlClientImplementor sqlClient,
            DeleteMode mode,
            DissociateAction defaultDissociateAction, Map<ImmutableProp, DissociateAction> dissociateActionMap
    ) {
        this.sqlClient = sqlClient;
        this.mode = mode;
        this.defaultDissociateAction = defaultDissociateAction;
        this.dissociateActionMap = dissociateActionMap;
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
    }

    @Override
    public DeleteMode getMode() {
        return mode;
    }

    @Override
    public DissociateAction getDissociateAction(ImmutableProp backReferenceProp) {
        DissociateAction dissociateAction = dissociateActionMap.get(backReferenceProp);
        if (dissociateAction != null) {
            return dissociateAction;
        }
        return defaultDissociateAction;
    }
}
