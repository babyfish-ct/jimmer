package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public interface Ast {

    void accept(@NotNull AstVisitor visitor);

    void renderTo(@NotNull SqlBuilder builder);

    static Ast from(Selection<?> selection, RootTableResolver resolver) {
        if (selection instanceof Table<?>) {
            return TableProxies.resolve((Table<?>) selection, resolver);
        }
        return AbstractTypedEmbeddedPropExpression.unwrap(selection);
    }
}
