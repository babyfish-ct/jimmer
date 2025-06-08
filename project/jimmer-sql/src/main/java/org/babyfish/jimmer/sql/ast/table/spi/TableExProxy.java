package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;

public interface TableExProxy<E, T extends Table<E>> extends TableProxy<E>, TableEx<E> {
}
