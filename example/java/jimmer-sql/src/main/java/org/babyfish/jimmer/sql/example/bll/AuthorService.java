package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.example.dal.AuthorRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @GetMapping("/authors/simple")
    public List<@FetchBy("SIMPLE_AUTHOR_FETCHER") Author> findSimpleAuthors(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Gender gender
    ) {
        return authorRepository.findAuthors(
                firstName,
                lastName,
                gender,
                SIMPLE_AUTHOR_FETCHER
        );
    }

    @GetMapping("/authors/complex")
    public List<@FetchBy("COMPLEX_AUTHOR_FETCHER") Author> findComplexAuthors(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Gender gender
    ) {
        return authorRepository.findAuthors(
                firstName,
                lastName,
                gender,
                COMPLEX_AUTHOR_FETCHER
        );
    }

    private static final Fetcher<Author> SIMPLE_AUTHOR_FETCHER =
            AuthorFetcher.$
                    .firstName()
                    .lastName();

    private static final Fetcher<Author> COMPLEX_AUTHOR_FETCHER =
            AuthorFetcher.$
                    .allScalarFields()
                    .books(
                            BookFetcher.$
                                    .allScalarFields()
                                    .store(
                                            BookStoreFetcher.$
                                                    .allScalarFields()
                                                    .avgPrice()
                                    )
                    );
}
