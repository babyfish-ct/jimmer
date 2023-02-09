package org.babyfish.jimmer.dto;

import lombok.Data;
import org.babyfish.jimmer.mapstruct.ImmutableFactory;
import org.babyfish.jimmer.mapstruct.JimmerMapperConfig;
import org.babyfish.jimmer.model.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Data
public class BookDto {

    private static final BookDtoMapper MAPPER = Mappers.getMapper(BookDtoMapper.class);

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

    @Mapper(config = JimmerMapperConfig.class)
    interface BookDtoMapper {

        void fillBookDraft(BookDto dto, @MappingTarget BookDraft draft);

        default Book toBook(BookDto dto) {
            return ImmutableFactory.byDto(dto, Book.class, this::fillBookDraft);
        }

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        void fillBookStoreDraft(TargetOf_store dto, @MappingTarget BookStoreDraft draft);

        default BookStore toBookStore(TargetOf_store dto) {
            return ImmutableFactory.byDto(dto, BookStore.class, this::fillBookStoreDraft);
        }

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        void fillAuthorDraft(TargetOf_authors dto, @MappingTarget AuthorDraft draft);

        default Author toAuthor(TargetOf_authors dto) {
            return ImmutableFactory.byDto(dto, Author.class, this::fillAuthorDraft);
        }
    }
}
