package org.babyfish.jimmer.sql.example.graphql.entities.input;

import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.spring.model.Input;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreProps;
import org.springframework.lang.Nullable;

public class BookStoreInput implements Input<BookStore> {

    private static final ImmutableConverter<BookStore, BookStoreInput> CONVERTER =
            ImmutableConverter
                    .forFields(BookStore.class, BookStoreInput.class)
                    .map(BookStoreProps.ID, mapping -> {
                        mapping.useIf(input -> input.id != null);
                    })
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

    /**
     * The only value of this class is the method `toEntity`,
     * which converts the current static InputDTO into a dynamic entity object.
     *
     * If the code does not explicitly use private fields, it will cause Intellij to warn,
     * and it is necessary to provide a view for the debugger,
     * so define this toString method
     *
     * @return
     */
    public BookStore toEntity() {
        return CONVERTER.convert(this);
    }
}
