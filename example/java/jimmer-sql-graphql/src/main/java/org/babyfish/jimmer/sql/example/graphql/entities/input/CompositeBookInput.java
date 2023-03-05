package org.babyfish.jimmer.sql.example.graphql.entities.input;

import lombok.Data;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.Gender;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
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
    private StoreTarget store;

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
        BookStore toBookStore(StoreTarget target);

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        Author toAuthor(AuthorTarget target);
    }

    @Data
    public static class StoreTarget {

        private String name;

        @Nullable
        private String website;
    }

    @Data
    public static class AuthorTarget {

        private String firstName;

        private String lastName;

        private Gender gender;
    }
}
