package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

public interface FetcherSelection<T> extends Selection<T> {

    Fetcher<?> getFetcher();
}
