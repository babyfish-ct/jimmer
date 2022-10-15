package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.filter.Filter;

/**
 * Only used by jimmer-sql-kotlin internally
 */
public interface TypeAwareFilter extends Filter<Props> {

    ImmutableType getImmutableType();
}
