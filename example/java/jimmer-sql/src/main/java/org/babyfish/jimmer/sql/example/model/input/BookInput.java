package org.babyfish.jimmer.sql.example.model.input;

import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.spring.model.Input;
import org.babyfish.jimmer.sql.example.model.Author;
import org.babyfish.jimmer.sql.example.model.Book;
import org.babyfish.jimmer.sql.example.model.BookProps;
import org.babyfish.jimmer.sql.example.model.BookStore;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

public class BookInput implements Input<Book> {

    private static final ImmutableConverter<Book, BookInput> CONVERTER =
        ImmutableConverter
                .forFields(Book.class, BookInput.class)
                .map(BookProps.ID, mapping -> {
                    mapping.useIf(input -> input.id != null);
                })
                .map(BookProps.STORE, "storeId", mapping -> {
                    mapping.valueConverter(value ->
                            ImmutableObjects.makeIdOnly(BookStore.class, value)
                    );
                })
                .mapList(BookProps.AUTHORS, "authorIds", mapping -> {
                    mapping.elementConverter(element ->
                            ImmutableObjects.makeIdOnly(Author.class, element)
                    );
                })
                .build();

    @Nullable
    private final Long id;

    private final String name;

    private final int edition;

    private final BigDecimal price;

    @Nullable
    private final Long storeId;

    private final List<Long> authorIds;

    public BookInput(
            @Nullable Long id,
            String name,
            int edition,
            BigDecimal price,
            @Nullable Long storeId,
            List<Long> authorIds
    ) {
        this.id = id;
        this.name = name;
        this.edition = edition;
        this.price = price;
        this.storeId = storeId;
        this.authorIds = authorIds;
    }

    @Override
    public Book toEntity() {
        return CONVERTER.convert(this);
    }

    @Override
    public String toString() {
        return "BookInput{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", edition=" + edition +
                ", price=" + price +
                ", storeId=" + storeId +
                ", authorIds=" + authorIds +
                '}';
    }
}
