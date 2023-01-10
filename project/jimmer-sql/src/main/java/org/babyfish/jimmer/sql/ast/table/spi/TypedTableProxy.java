package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.table.TableEx;

public interface TypedTableProxy<E, TEX extends TableEx<E>> extends TableProxy<E> {

    @Override
    TEX asTableEx();
}
