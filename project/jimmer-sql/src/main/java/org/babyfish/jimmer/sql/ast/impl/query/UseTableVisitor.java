package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

class UseTableVisitor extends AstVisitor {

    UseTableVisitor(SqlBuilder sqlBuilder) {
        super(sqlBuilder);
    }

    @Override
    public void visitTableReference(Table<?> table, ImmutableProp prop) {

        TableImplementor<?> tableImpl = TableImplementor.unwrap(table);
        if (prop == null) {
            if (tableImpl.getImmutableType().getSelectableProps().size() > 1) {
                use(tableImpl);
            }
        } else if (prop.isId()) {
            use(tableImpl.getParent());
        } else {
            use(tableImpl);
        }
    }

    private void use(TableImplementor<?> table) {
        if (table != null) {
            getSqlBuilder().useTable(table);
            use(table.getParent());
        }
    }
}
