package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.fetcher.compiler.FetcherCompiler;
import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.junit.jupiter.api.Test;

public class FetcherCompilerTest extends Tests {

    @Test
    public void testIssue672() {
        Fetcher<Book> fetcher = BookFetcher.$
                .storeId()
                .authors(
                        AuthorFetcher.$
                                .allScalarFields()
                );
        Fetcher<?> fetcher2 = FetcherCompiler.compile(fetcher.toString());
        assertContentEquals(
                "org.babyfish.jimmer.sql.model.Book { " +
                        "--->id, " +
                        "--->storeId, " +
                        "--->authors { " +
                        "--->--->id, " +
                        "--->--->firstName, " +
                        "--->--->lastName, " +
                        "--->--->gender " +
                        "--->}, " +
                        "--->@implicit store " +
                        "}",
                fetcher2
        );
    }
}
