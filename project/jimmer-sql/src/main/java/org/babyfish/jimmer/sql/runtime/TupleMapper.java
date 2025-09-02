package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.ast.Selection;

import java.util.List;

public interface TupleMapper<T> extends TupleCreator<T> {

    List<Selection<?>> getSelections();
}
