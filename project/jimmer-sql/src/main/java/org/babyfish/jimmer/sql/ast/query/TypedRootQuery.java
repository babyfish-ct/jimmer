package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Executable;

import java.util.List;

public interface TypedRootQuery<R> extends Executable<List<R>> {

    TypedRootQuery<R> union(TypedRootQuery<R> other);

    TypedRootQuery<R> unionAll(TypedRootQuery<R> other);

    TypedRootQuery<R> minus(TypedRootQuery<R> other);

    TypedRootQuery<R> intersect(TypedRootQuery<R> other);
}
