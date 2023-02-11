package org.babyfish.jimmer.sql.example.graphql.entities.input;

import lombok.Data;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Data
public class BookStoreInput implements Input<BookStore> {

    private final static Converter CONVERTER = Mappers.getMapper(Converter.class);

    @Nullable
    private Long id;

    private String name;

    @Nullable
    private String website;

    @Override
    public BookStore toEntity() {
        return CONVERTER.toBookStore(this);
    }

    @Mapper
    interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        BookStore toBookStore(BookStoreInput input);
    }
}
