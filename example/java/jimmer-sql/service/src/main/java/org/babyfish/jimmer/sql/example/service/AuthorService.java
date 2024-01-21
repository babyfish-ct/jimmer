package org.babyfish.jimmer.sql.example.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.example.repository.AuthorRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.service.dto.AuthorInput;
import org.babyfish.jimmer.sql.example.service.dto.AuthorSpecification;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.SaveErrorCode;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Why add spring web annotations to the service class?
 *
 * The success and popularity of rich client technologies represented by React, Vue and Angular
 * have greatly reduced the significance of the Controller layer on the spring server side.
 *
 * Moreover, over-bloated code structures are not conducive to demonstrating the capabilities
 * of the framework with small examples. Therefore, this example project no longer adheres to
 * dogmatism and directly adds spring web annotations to the service class.
 */
@RestController
@RequestMapping("/author")
@Transactional
public class AuthorService implements Fetchers {

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
            AuthorSpecification specification,
            @RequestParam(defaultValue = "firstName asc, lastName asc") String sortCode
    ) {
        return authorRepository.find(
                specification,
                SortUtils.toSort(sortCode),
                DEFAULT_FETCHER
        );
    }

    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("COMPLEX_FETCHER") Author findComplexAuthor(
            @PathVariable("id") long id
    ) {
        return authorRepository.findNullable(id, COMPLEX_FETCHER);
    }

    private static final Fetcher<Author> SIMPLE_FETCHER =
            AUTHOR_FETCHER
                    .firstName()
                    .lastName();

    private static final Fetcher<Author> DEFAULT_FETCHER =
            AUTHOR_FETCHER
                    .allScalarFields();

    private static final Fetcher<Author> COMPLEX_FETCHER =
            AUTHOR_FETCHER
                    .allScalarFields()
                    .books(
                            BOOK_FETCHER
                                    .allScalarFields()
                                    .tenant(false)
                                    .store(
                                            BOOK_STORE_FETCHER
                                                    .allScalarFields()
                                                    .avgPrice()
                                    )
                    );

    @PutMapping
    public Author saveAuthor(@RequestBody AuthorInput input) throws SaveException {
        return authorRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteAuthor(@PathVariable("id") long id) {
        authorRepository.deleteById(id);
    }
}
