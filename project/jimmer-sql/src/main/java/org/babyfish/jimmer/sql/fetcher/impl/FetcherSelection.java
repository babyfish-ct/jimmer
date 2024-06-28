package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface FetcherSelection<T> extends Selection<T> {

    FetchPath getPath();

    Fetcher<?> getFetcher();

    PropExpression.Embedded<?> getEmbeddedPropExpression();

    @Nullable
    Function<?, ?> getConverter();
}
