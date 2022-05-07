package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.runtime.SqlBuilder;

interface Ast {

    void accept(AstVisitor visitor);

    void renderTo(SqlBuilder builder);
}
