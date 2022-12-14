package org.babyfish.jimmer.client.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.model.Author;
import org.babyfish.jimmer.client.model.AuthorFetcher;
import org.babyfish.jimmer.client.model.BookFetcher;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

public interface AuthorService {

    Fetcher<Author> AUTHOR_FETCHER = AuthorFetcher.$
            .allScalarFields()
            .gender(false)
            .books(
                    BookFetcher.$.name()
            );

    @GetMapping("/author/{id}")
    @FetchBy("AUTHOR_FETCHER") @Nullable Author findAuthor(@PathVariable("id") long id);
}
