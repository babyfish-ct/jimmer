package org.babyfish.jimmer.sql.example.model.input;

import lombok.Data;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.example.model.Author;
import org.babyfish.jimmer.sql.example.model.Book;
import org.babyfish.jimmer.sql.example.model.BookStore;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BookInput implements Input<Book> {

    private static final Converter CONVERTER = Mappers.getMapper(Converter.class);

    @Nullable
    private Long id;

    private String name;

    private int edition;

    private BigDecimal price;

    @Nullable
    private Long storeId;

    private List<Long> authorIds;

    @Override
    public Book toEntity() {
        return CONVERTER.toBook(this);
    }

    @Mapper
    interface Converter {

        @Mapping(target = "store", source = "storeId")
        @Mapping(target = "authors", source = "authorIds")
        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        Book toBook(BookInput input);

        default BookStore toBookStore(long id) {
            return ImmutableObjects.makeIdOnly(BookStore.class, id);
        }

        default Author toAuthor(long id) {
            return ImmutableObjects.makeIdOnly(Author.class, id);
        }
    }
}
