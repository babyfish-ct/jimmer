package org.babyfish.jimmer.sql.example.graphql.business;

import org.babyfish.jimmer.sql.example.graphql.repository.AuthorRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.input.AuthorInput;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * A real project should be a three-tier architecture consisting
 * of repository, service, and controller.
 *
 * This demo has no business logic, its purpose is only to tell users
 * how to use jimmer with the <b>least</b> code. Therefore, this demo
 * does not follow this convention, and let services be directly
 * decorated by `@Controller`, not `@Service`.
 */
@Controller
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    // --- Query ---

    @QueryMapping
    public List<Author> authors(@Argument @Nullable String name) {
        return authorRepository.findByName(name);
    }

    // --- Mutation ---

    @MutationMapping
    public Author saveAuthor(@Argument AuthorInput input) {
        return authorRepository.save(input);
    }

    @MutationMapping
    public int deleteAuthor(@Argument long id) {
        authorRepository.deleteById(id);
        // GraphQL requires return value,
        // but `deleteById` of spring-data does not support value.
        // Is there a better design?
        return 1;
    }
}
