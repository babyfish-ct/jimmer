package org.babyfish.jimmer.sql.ast.query.specification;

import org.babyfish.jimmer.sql.ast.table.Table;

public interface Specification<E, T extends Table<E>> {

    void applyTo(SpecificationArgs<E, T> args);
}
