package org.babyfish.jimmer.sql.example.graphql.entities.input;

import lombok.Data;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.Chapter;
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

    private List<TargetOfChapters> chapters;

    @Override
    public Book toEntity() {
        return CONVERTER.toBook(this);
    }

    @Data
    public static class TargetOfChapters {
        private int index;
        private String title;
    }

    @Mapper
    interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        @Mapping(target = "store", source = "storeId")
        @Mapping(target = "authors", source = "authorIds")
        Book toBook(BookInput input);

        default BookStore toBookStore(long id) {
            return ImmutableObjects.makeIdOnly(BookStore.class, id);
        }

        default Author toAuthor(long id) {
            return ImmutableObjects.makeIdOnly(Author.class, id);
        }

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        Chapter toChapter(TargetOfChapters input);
    }
}
