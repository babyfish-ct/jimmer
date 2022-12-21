package org.babyfish.jimmer.sql.example.graphql.entities.input;

import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.spring.model.Input;
import org.babyfish.jimmer.sql.example.graphql.entities.*;
import org.springframework.lang.Nullable;

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
