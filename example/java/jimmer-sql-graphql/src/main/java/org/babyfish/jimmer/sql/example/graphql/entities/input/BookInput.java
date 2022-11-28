package org.babyfish.jimmer.sql.example.graphql.entities.input;

import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.example.graphql.entities.*;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.List;

public class BookInput {

    private static final ImmutableConverter<Book, BookInput> BOOK_CONVERTER =
            ImmutableConverter
                    .newBuilder(Book.class, BookInput.class)
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
                    .autoMapOtherScalars(true)
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

    public Book toBook() {
        return BOOK_CONVERTER.convert(this);
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
