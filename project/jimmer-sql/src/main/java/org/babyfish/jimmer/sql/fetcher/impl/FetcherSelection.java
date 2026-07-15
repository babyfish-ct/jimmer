package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.DtoMetadata;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface FetcherSelection<T> extends Selection<T> {

    FetchPath getPath();

    Fetcher<?> getFetcher();

    @Nullable
    default DtoMetadata<?, ?> getDtoMetadata() {
        return null;
    }

    PropExpression.Embedded<?> getEmbeddedPropExpression();

    @Nullable
    Function<?, ?> getConverter();
}
