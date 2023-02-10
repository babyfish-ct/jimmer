package org.babyfish.jimmer.dto;

import lombok.Data;
import org.babyfish.jimmer.model.*;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Data
public class BookDto {

    private static final BookDtoMapper MAPPER = Mappers.getMapper(BookDtoMapper.class);

    @Nullable
    private final String name;

    private final String edition;

    private final String price;

    private final TargetOf_store store;

    private final List<TargetOf_authors> authors;

    @Data
    public static class TargetOf_store {
        private final String name;
    }

    @Data
    public static class TargetOf_authors {
        private final String name;
    }

    public Book toEntity() {
        return MAPPER.toBook(this);
    }

    @Mapper
    interface BookDtoMapper {

        Book toBook(BookDto dto);

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        BookStore toBookStore(TargetOf_store dto);

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        Author toAuthor(TargetOf_authors dto);
    }
}
