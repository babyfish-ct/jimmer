package org.babyfish.jimmer.example.cloud.author;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.example.cloud.model.Author;
import org.babyfish.jimmer.example.cloud.model.AuthorFetcher;
import org.babyfish.jimmer.example.cloud.model.BookFetcher;
import org.babyfish.jimmer.example.cloud.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @GetMapping("/author/{id}")
    public @FetchBy("SIMPLE_FETCHER") Author findAuthor(
            @PathVariable("id") long id
    ) {
        return authorRepository.findNullable(id, SIMPLE_FETCHER);
    }

    @GetMapping("/author/{id}/detail")
    public @FetchBy("COMPLEX_FETCHER") Author findAuthorDetail(
            @PathVariable("id") long id
    ) {
        return authorRepository.findNullable(id, COMPLEX_FETCHER);
    }

    public static final Fetcher<Author> SIMPLE_FETCHER =
            AuthorFetcher.$
                    .firstName()
                    .lastName();

    public static final Fetcher<Author> COMPLEX_FETCHER =
            AuthorFetcher.$
                    .allScalarFields()
                    .books(
                            BookFetcher.$
                                    .allScalarFields()
                                    .store(
                                            BookStoreFetcher.$
                                                    .allScalarFields()
                                    )
                    );
}
