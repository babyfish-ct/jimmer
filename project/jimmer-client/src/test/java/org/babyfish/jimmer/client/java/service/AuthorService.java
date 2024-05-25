package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.common.RequestMapping;
import org.babyfish.jimmer.client.java.model.*;
import org.babyfish.jimmer.client.common.GetMapping;
import org.babyfish.jimmer.client.common.PathVariable;
import org.babyfish.jimmer.client.meta.Api;
import org.babyfish.jimmer.client.runtime.Operation;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

@Api("authorService")
public interface AuthorService {

    /**
     * Simple author DTO
     */
    Fetcher<Author> SIMPLE_FETCHER = AuthorFetcher.$.fullName();

    Fetcher<Author> ISSUE_574_FETCHER = AuthorFetcher.$.gender();

    @GetMapping("/author/simple/{id}")
    @Api
    @FetchBy("SIMPLE_FETCHER") @Nullable Author findSimpleAuthor(@PathVariable("id") long id);

    @GetMapping("/author/issue_574/{id}")
    @Api
    @FetchBy("ISSUE_574_FETCHER") @Nullable Author findIssue574Author(@PathVariable("id") long id);

    @GetMapping("/author/image/{id}")
    @Api
    StreamingResponseBody findAuthorImage(@PathVariable("id") long id);
}
