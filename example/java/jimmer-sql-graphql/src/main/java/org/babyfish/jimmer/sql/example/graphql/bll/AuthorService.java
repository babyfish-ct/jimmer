package org.babyfish.jimmer.sql.example.graphql.bll;

import org.babyfish.jimmer.sql.example.graphql.dal.AuthorRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorProps;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.input.AuthorInput;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

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

    // --- Association ---

    @BatchMapping
    public Map<Author, List<Book>> books(
            List<Author> authors
    ) {
        return authorRepository.graphql().load(AuthorProps.BOOKS, authors);
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
