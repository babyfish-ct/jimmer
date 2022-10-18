package org.babyfish.jimmer.sql.example.controller;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.common.CommonEntityProps;
import org.babyfish.jimmer.sql.example.model.common.TenantAwareProps;
import org.babyfish.jimmer.sql.fluent.Fluent;
import org.babyfish.jimmer.sql.meta.Column;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class BookController {

    private final JSqlClient sqlClient;

    public BookController(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @GetMapping("/stores")
    public List<BookStore> stores(
            @RequestParam(defaultValue = "false") boolean fetch
    ) {
        if (fetch) {
            return sqlClient.getEntities().findAll(
                    BookStoreFetcher.$
                            .allScalarFields()
                            .avgPrice()
                            .books(
                                    BookFetcher.$
                                            .allScalarFields()
                                            .authors(
                                                    AuthorFetcher.$
                                                            .allScalarFields()
                                            )
                            ),
                    BookStoreProps.NAME.asc()
            );
        }
        return sqlClient.getEntities().findAll(
                BookStore.class,
                BookStoreProps.NAME.asc()
        );
    }

    @GetMapping("/books")
    public Page<Book> books(
            @RequestParam(defaultValue = "false") boolean fetch,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String storeName,
            @RequestParam(defaultValue = "") String authorName,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize
    ) {

        Fluent fluent = sqlClient.createFluent();
        BookTable book = new BookTable();
        AuthorTableEx author = new AuthorTableEx();

        ConfigurableRootQuery<BookTable, Book> query = fluent
                .query(book)
                .whereIf(
                        name != null && !name.isEmpty(),
                        () -> book.name().ilike(name)
                )
                .whereIf(
                        storeName != null && !storeName.isEmpty(),
                        () -> book.store().name().ilike(storeName)
                )
                .whereIf(
                        authorName != null && !authorName.isEmpty(),
                        () -> book.id().in(fluent
                                .subQuery(author)
                                .where(
                                        Predicate.or(
                                                author.firstName().ilike(authorName),
                                                author.lastName().ilike(authorName)
                                        )
                                )
                                .select(author.books().id())
                        )
                )
                .select(
                        fetch ?
                                book.fetch(
                                        BookFetcher.$
                                                .allScalarFields()
                                                .store(
                                                        BookStoreFetcher.$
                                                                .allScalarFields()
                                                                .avgPrice()
                                                )
                                                .authors(
                                                        AuthorFetcher.$
                                                                .allScalarFields()
                                                )
                                ) :
                                book
                );

        int rowCount = query.count();
        int pageCount = (rowCount + pageSize - 1) / pageSize;
        List<Book> books = query
                .limit(pageSize, pageIndex * pageSize)
                .execute();
        return new Page<>(books, rowCount, pageCount);
    }

    @GetMapping("/authors")
    public List<Author> authors(
            @RequestParam(defaultValue = "false") boolean fetch,
            @RequestParam(defaultValue = "") String firstName,
            @RequestParam(defaultValue = "") String lastName,
            @RequestParam(required = false) Gender gender
    ) {
        Author author = AuthorDraft.$.produce(draft -> {
            if (!firstName.isEmpty()) {
                draft.setFirstName(firstName);
            }
            if (!lastName.isEmpty()) {
                draft.setLastName(lastName);
            }
            if (gender != null) {
                draft.setGender(gender);
            }
        });
        return sqlClient.getEntities().findByExample(
                Example.of(author)
                        .ilike(AuthorProps.FIRST_NAME)
                        .ilike(AuthorProps.LAST_NAME),
                fetch ?
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
                                ) :
                        null
        );
    }

    @Transactional
    @PutMapping("book")
    public Book saveBook(@RequestBody Book book) {
        validateRequiredFields(
                book,
                CommonEntityProps.CREATED_TIME,
                CommonEntityProps.MODIFIED_TIME,
                TenantAwareProps.TENANT
        );
        return sqlClient.getEntities().save(book).getModifiedEntity();
    }

    private void validateRequiredFields(
            Object immutable,
            TypedProp<?, ?> ... exceptedProps
    ) {
        if (!(immutable instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("The argument is not immutable object");
        }
        validateRequiredFieldsImpl(
                (ImmutableSpi) immutable,
                Arrays.stream(exceptedProps).map(TypedProp::unwrap).collect(Collectors.toSet())
        );
    }

    private void validateRequiredFieldsImpl(
            ImmutableSpi spi,
            Set<ImmutableProp> exceptedProps
    ) {
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (prop.isId() && !prop.isTransient() && !prop.isNullable() && prop.getStorage() instanceof Column && !exceptedProps.contains(prop)) {
                if (!spi.__isLoaded(prop.getId())) {
                    throw new IllegalArgumentException(
                            "The required property \"" +
                                    prop +
                                    "\" of an object is not loaded"
                    );
                }
            } else if (prop.isAssociation(TargetLevel.ENTITY) && spi.__isLoaded(prop.getId())) {
                Object value = spi.__get(prop.getId());
                if (value instanceof Collection<?>) {
                    for (Object target : (Collection<?>)value) {
                        validateRequiredFieldsImpl((ImmutableSpi)target, exceptedProps);
                    }
                } else if (value != null) {
                    validateRequiredFieldsImpl((ImmutableSpi)value, exceptedProps);
                }
            }
        }
    }
}
