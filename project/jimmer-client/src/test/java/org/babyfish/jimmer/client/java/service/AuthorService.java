package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.java.model.Author;
import org.babyfish.jimmer.client.meta.common.GetMapping;
import org.babyfish.jimmer.client.meta.common.PathVariable;
import org.babyfish.jimmer.client.java.model.AuthorFetcher;
import org.babyfish.jimmer.client.java.model.BookFetcher;
import org.babyfish.jimmer.client.java.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

public interface AuthorService {

    Fetcher<Author> SIMPLE_FETCHER = AuthorFetcher.$.firstName().lastName();

    Fetcher<Author> COMPLEX_FETCHER = AuthorFetcher.$
            .allScalarFields()
            .gender(false)
            .books(
                    BookFetcher.$
                            .allScalarFields()
                            .store(
                                    BookStoreFetcher.$
                                            .allScalarFields()
                            )
            );

    @GetMapping("/author/simple/{id}")
    @FetchBy("SIMPLE_FETCHER") @Nullable Author findSimpleAuthor(@PathVariable("id") long id);

    @GetMapping("/author/complex/{id}")
    @FetchBy("COMPLEX_FETCHER") @Nullable Author findComplexAuthor(@PathVariable("id") long id);
}
