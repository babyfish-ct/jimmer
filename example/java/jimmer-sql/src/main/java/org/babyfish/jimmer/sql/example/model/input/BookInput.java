package org.babyfish.jimmer.sql.example.model.input;

import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.example.model.Author;
import org.babyfish.jimmer.sql.example.model.Book;
import org.babyfish.jimmer.sql.example.model.BookProps;
import org.babyfish.jimmer.sql.example.model.BookStore;

import java.math.BigDecimal;
import java.util.List;

public class BookInput {

    private static final ImmutableConverter<Book, BookInput> BOOK_CONVERTER =
        ImmutableConverter
                .newBuilder(Book.class, BookInput.class)
                .map(BookProps.ID, mapping -> {
                    mapping.useIf(input -> input.id != null);
                })
                .map(BookProps.STORE, mapping -> {
                    mapping.valueConverter(value ->
                            ImmutableObjects.makeIdOnly(BookStore.class, value)
                    );
                })
                .mapList(BookProps.AUTHORS, mapping -> {
                    mapping.elementConverter(element ->
                            ImmutableObjects.makeIdOnly(Author.class, element)
                    );
                })
                .autoMapOtherScalars() // name, edition, price
                .build();

    private Long id;

    private String name;

    private int edition;

    private BigDecimal price;

    private Long storeId;

    private List<Long> authorIds;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getEdition() {
        return edition;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Long getStoreId() {
        return storeId;
    }

    public List<Long> getAuthorIds() {
        return authorIds;
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
