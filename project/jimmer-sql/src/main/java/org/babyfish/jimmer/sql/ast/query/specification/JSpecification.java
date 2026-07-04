package org.babyfish.jimmer.sql.ast.query.specification;

import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@ApiIgnore
public interface JSpecification<E, T extends TableLike<E>> extends Specification<E> {

    @SafeVarargs
    @Nullable
    static <E> JSpecification<E, Table<E>> and(JSpecification<? extends E, ?>... specifications) {
        return Specifications.compose(Specifications.Operator.AND, Arrays.asList(specifications));
    }

    @Nullable
    static <E> JSpecification<E, Table<E>> and(
            Iterable<? extends JSpecification<? extends E, ?>> specifications
    ) {
        return Specifications.compose(Specifications.Operator.AND, specifications);
    }

    @SafeVarargs
    @Nullable
    static <E> JSpecification<E, Table<E>> or(JSpecification<? extends E, ?>... specifications) {
        return Specifications.compose(Specifications.Operator.OR, Arrays.asList(specifications));
    }

    @Nullable
    static <E> JSpecification<E, Table<E>> or(
            Iterable<? extends JSpecification<? extends E, ?>> specifications
    ) {
        return Specifications.compose(Specifications.Operator.OR, specifications);
    }

    @Nullable
    static <E> JSpecification<E, Table<E>> not(
            @Nullable JSpecification<? extends E, ?> specification
    ) {
        return Specifications.negate(specification);
    }

    void applyTo(SpecificationArgs<E, T> args);
}
