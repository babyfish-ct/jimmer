package org.babyfish.jimmer.spring.java.model;

import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.client.Doc;
import org.babyfish.jimmer.client.ExportFields;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

@ExportFields
public class BookInput implements Input<Book> {

    private static final ImmutableConverter<Book, BookInput> CONVERTER =
            ImmutableConverter
                    .forFields(Book.class, BookInput.class)
                    .map(
                            BookProps.STORE,
                            "storeId",
                            value -> ImmutableObjects.makeIdOnly(BookStore.class, value)
                    )
                    .mapList(
                            BookProps.AUTHORS,
                            "authorIds",
                            value -> ImmutableObjects.makeIdOnly(Author.class, value)
                    )
                    .build();

    private final String name;

    private final int edition;

    private final BigDecimal price;

    @Doc("Null is allowed")
    @Nullable
    private final Long storeId;

    private final List<Long> authorIds;

    public BookInput(
            String name,
            int edition,
            BigDecimal price,
            @Nullable Long storeId,
            List<Long> authorIds
    ) {
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
                "name='" + name + '\'' +
                ", edition=" + edition +
                ", price=" + price +
                ", storeId=" + storeId +
                ", authorIds=" + authorIds +
                '}';
    }
}
