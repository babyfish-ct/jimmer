package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.example.dal.AuthorRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/author")
@Transactional
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @GetMapping("/simpleList")
    public List<@FetchBy("SIMPLE_FETCHER") Author> findSimpleAuthors() {
        return authorRepository.findAll(SIMPLE_FETCHER, AuthorProps.FIRST_NAME, AuthorProps.LAST_NAME);
    }

    @GetMapping("/list")
    public List<@FetchBy("DEFAULT_FETCHER") Author> findAuthors(
            @RequestParam(defaultValue = "firstName asc, lastName asc") String sortCode,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Gender gender
    ) {
        return authorRepository.findByFirstNameAndLastNameAndGender(
                SortUtils.toSort(sortCode),
                firstName,
                lastName,
                gender,
                DEFAULT_FETCHER
        );
    }

    @GetMapping("/complexList")
    public List<@FetchBy("COMPLEX_FETCHER") Author> findComplexAuthors(
            @RequestParam(defaultValue = "firstName asc, lastName asc") String sortCode,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Gender gender
    ) {
        return authorRepository.findByFirstNameAndLastNameAndGender(
                SortUtils.toSort(sortCode),
                firstName,
                lastName,
                gender,
                COMPLEX_FETCHER
        );
    }

    private static final Fetcher<Author> SIMPLE_FETCHER =
            AuthorFetcher.$
                    .firstName()
                    .lastName();

    private static final Fetcher<Author> DEFAULT_FETCHER =
            AuthorFetcher.$
                    .allScalarFields();

    private static final Fetcher<Author> COMPLEX_FETCHER =
            AuthorFetcher.$
                    .allScalarFields()
                    .books(
                            BookFetcher.$
                                    .allScalarFields()
                                    .tenant(false)
                                    .store(
                                            BookStoreFetcher.$
                                                    .allScalarFields()
                                                    .avgPrice()
                                    )
                    );

    @PutMapping
    public Author saveAuthor(AuthorInput input) {
        return authorRepository.save(input);
    }
}
