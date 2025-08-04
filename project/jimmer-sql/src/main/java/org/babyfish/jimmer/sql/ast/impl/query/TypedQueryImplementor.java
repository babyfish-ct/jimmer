package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.List;

public interface TypedQueryImplementor extends Ast {

    List<Selection<?>> getSelections();

    JSqlClientImplementor getSqlClient();
}
