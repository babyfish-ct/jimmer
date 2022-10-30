package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public interface TableSelection {

    ImmutableType getImmutableType();

    void renderSelection(ImmutableProp prop, SqlBuilder builder);
}
