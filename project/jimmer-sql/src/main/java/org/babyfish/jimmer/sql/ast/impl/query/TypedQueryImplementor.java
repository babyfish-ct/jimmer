package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;

import java.util.List;

interface TypedQueryImplementor extends Ast {

    List<Selection<?>> getSelections();
}
