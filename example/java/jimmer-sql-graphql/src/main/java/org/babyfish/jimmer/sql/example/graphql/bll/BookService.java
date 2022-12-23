package org.babyfish.jimmer.sql.example.graphql.bll;

import org.babyfish.jimmer.sql.example.graphql.dal.BookRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.*;
import org.babyfish.jimmer.sql.example.graphql.entities.input.BookInput;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Controller
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // --- Query ---

    @QueryMapping
    public List<Book> books(
            @Argument @Nullable String name,
            @Argument @Nullable String storeName,
            @Argument @Nullable String authorName
    ) {
        return bookRepository.find(name, storeName, authorName);
    }

    // --- Association ---

    @BatchMapping
    public Map<Book, BookStore> store(Collection<Book> books) {
        return bookRepository.graphql().load(BookProps.STORE, books);
    }

    @BatchMapping
    public Map<Book, List<Author>> authors(List<Book> books) {
        return bookRepository.graphql().load(BookProps.AUTHORS, books);
    }

    // --- Mutation ---

    @MutationMapping
    @Transactional
    public Book saveBook(@Argument BookInput input) {
        return bookRepository.save(input);
    }

    @MutationMapping
    @Transactional
    public int deleteBook(@Argument long id) {
        bookRepository.deleteById(id);
        // GraphQL requires return value,
        // but `deleteById` of spring-data does not support value.
        // Is there a better design?
        return 1;
    }
}
