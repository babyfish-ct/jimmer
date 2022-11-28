package org.babyfish.jimmer.sql.example.graphql.entities.input;

import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreProps;
import org.springframework.lang.Nullable;

public class BookStoreInput {

    private static final ImmutableConverter<BookStore, BookStoreInput> BOOK_STORE_CONVERTER =
            ImmutableConverter
                    .newBuilder(BookStore.class, BookStoreInput.class)
                    .map(BookStoreProps.ID, mapping -> {
                        mapping.useIf(input -> input.id != null);
                    })
                    .autoMapOtherScalars(true)
                    .build();

    @Nullable
    private final Long id;

    private final String name;

    @Nullable
    public final String website;

    public BookStoreInput(
            @Nullable Long id,
            String name,
            @Nullable String website
    ) {
        this.id = id;
        this.name = name;
        this.website = website;
    }

    public BookStore toBookStore() {
        return BOOK_STORE_CONVERTER.convert(this);
    }
}
