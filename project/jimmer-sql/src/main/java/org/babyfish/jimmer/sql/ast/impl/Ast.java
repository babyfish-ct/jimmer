package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public interface Ast {

    void accept(AstVisitor visitor);

    void renderTo(SqlBuilder builder);

    static Ast from(Selection<?> selection) {
        if (selection instanceof Table<?>) {
            return TableImplementor.unwrap((Table<?>) selection);
        }
        return (Ast) selection;
    }
}
