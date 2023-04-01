package org.babyfish.jimmer.sql.example.business;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.ThrowsAll;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.example.repository.AuthorRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.input.AuthorInput;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.SaveErrorCode;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A real project should be a three-tier architecture consisting
 * of repository, service, and controller.
 *
 * This demo has no business logic, its purpose is only to tell users
 * how to use jimmer with the <b>least</b> code. Therefore, this demo
 * does not follow this convention, and let services be directly
 * decorated by `@RestController`, not `@Service`.
 */
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

    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("COMPLEX_FETCHER") Author findComplexAuthor(
            @PathVariable("id") long id
    ) {
        return authorRepository.findNullable(id, COMPLEX_FETCHER);
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
    @ThrowsAll(SaveErrorCode.class)
    public Author saveAuthor(AuthorInput input) {
        return authorRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteAuthor(@PathVariable("id") long id) {
        authorRepository.deleteById(id);
    }
}
