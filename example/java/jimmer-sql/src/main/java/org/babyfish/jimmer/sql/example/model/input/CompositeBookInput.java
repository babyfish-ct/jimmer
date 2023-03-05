package org.babyfish.jimmer.sql.example.model.input;

import lombok.Data;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.example.model.*;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CompositeBookInput implements Input<Book> {

    private static final Converter CONVERTER = Mappers.getMapper(Converter.class);

    @Nullable
    private Long id;

    private String name;

    private int edition;

    private BigDecimal price;

    @Nullable
    private BookStoreTarget store;

    private List<AuthorTarget> authors;

    @Override
    public Book toEntity() {
        return CONVERTER.toBook(this);
    }

    @Mapper
    interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        Book toBook(CompositeBookInput input);

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        BookStore toBookStore(BookStoreTarget target);

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        Author toAuthor(AuthorTarget target);
    }

    @Data
    public static class BookStoreTarget {
        private String name;
        private String website;
    }

    @Data
    public static class AuthorTarget {
        private String firstName;
        private String lastName;
        private Gender gender;
    }
}
