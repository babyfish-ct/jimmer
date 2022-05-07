package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Selection;

import java.util.List;

interface TypedQueryImplementor extends Ast {

    List<Selection<?>> getSelections();
}
