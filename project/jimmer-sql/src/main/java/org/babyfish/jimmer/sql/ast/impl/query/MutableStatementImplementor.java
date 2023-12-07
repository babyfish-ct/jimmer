package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AstContext;

public interface MutableStatementImplementor {

    boolean hasVirtualPredicate();

    void resolveVirtualPredicate(AstContext ctx);
}
