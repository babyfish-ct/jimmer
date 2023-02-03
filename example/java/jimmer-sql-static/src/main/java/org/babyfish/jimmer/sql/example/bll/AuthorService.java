package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.example.dal.AuthorRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.dto.*;
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
    public List<SimpleAuthor> findSimpleAuthors() {
        return authorRepository.findAllStaticObjects(SimpleAuthor.class, AuthorProps.FIRST_NAME, AuthorProps.LAST_NAME);
    }

    @GetMapping("/list")
    public List<DefaultAuthor> findAuthors(
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
                DefaultAuthor.class
        );
    }

    @GetMapping("/complexList")
    public List<ComplexAuthor> findComplexAuthors(
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
                ComplexAuthor.class
        );
    }

    @PutMapping
    public Author saveAuthor(AuthorInput input) {
        return authorRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteAuthor(@PathVariable("id") long id) {
        authorRepository.deleteById(id);
    }
}
