package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;

public class UseTableVisitor extends AstVisitor {

    public UseTableVisitor(AstContext ctx) {
        super(ctx);
    }

    @Override
    public void visitTableReference(TableImplementor<?> table, ImmutableProp prop) {

        if (prop == null) {
            if (table.getImmutableType().getSelectableProps().size() > 1) {
                use(table);
            }
        } else if (prop.isId()) {
            getAstContext().useTableId(table);
            use(table.getParent());
        } else {
            use(table);
        }
    }

    private void use(TableImplementor<?> table) {
        if (table != null) {
            getAstContext().useTable(table);
            use(table.getParent());
        }
    }
}
