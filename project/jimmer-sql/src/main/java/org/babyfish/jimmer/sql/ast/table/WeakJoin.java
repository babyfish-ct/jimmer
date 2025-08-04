package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.io.Serializable;

/**
 * Note: Although this lambda interface implements the
 * `Serializable` interface, it has nothing to do with
 * serialization functionality.
 *
 * The purpose of this is to ensure that the interface
 * is classified as a Serializable lambda, forcing the
 * compiler to generate runtime-readable bytecode for
 * such lambda expressions. This allows jimmer to determine
 * whether the logic of two lambdas is equivalent.
 */
public interface WeakJoin<ST extends TableLike<?>, TT extends TableLike<?>> extends Serializable {

    Predicate on(ST source, TT target);
}
