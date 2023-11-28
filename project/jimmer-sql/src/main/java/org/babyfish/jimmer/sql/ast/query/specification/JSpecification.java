package org.babyfish.jimmer.sql.ast.query.specification;

import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.sql.ast.table.Table;

@ApiIgnore
public interface JSpecification<E, T extends Table<E>> extends Specification<E> {

    void applyTo(SpecificationArgs<E, T> args);
}
