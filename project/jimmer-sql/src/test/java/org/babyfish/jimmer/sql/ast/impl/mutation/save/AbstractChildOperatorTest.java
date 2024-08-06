package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteOptions;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.MutationPath;

import java.sql.Connection;
import java.util.HashMap;

public class AbstractChildOperatorTest extends AbstractMutationTest {

    static ChildTableOperator operator(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp manyToOneProp
    ) {
        return operator(sqlClient, con, manyToOneProp, DissociateAction.NONE);
    }

    static ChildTableOperator operator(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp manyToOneProp,
            DissociateAction dissociateAction
    ) {
        DeleteOptions options = new DeleteOptionsImpl(
                (JSqlClientImplementor) sqlClient,
                DeleteMode.PHYSICAL,
                dissociateAction
        );
        return new ChildTableOperator(
                new DeleteContext(
                        options,
                        con,
                        options.getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY ?
                            new MutationTrigger2() : null,
                        new HashMap<>(),
                        MutationPath.root(manyToOneProp.getTargetType())
                ).backPropOf(manyToOneProp)
        );
    }
}
