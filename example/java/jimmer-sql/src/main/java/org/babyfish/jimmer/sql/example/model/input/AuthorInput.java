package org.babyfish.jimmer.sql.example.model.input;

import lombok.Data;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.example.model.Author;
import org.babyfish.jimmer.sql.example.model.Gender;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Data
public class AuthorInput implements Input<Author> {

    private static final Converter CONVERTER = Mappers.getMapper(Converter.class);

    private Long id;

    private String firstName;

    private String lastName;

    private Gender gender;

    @Override
    public Author toEntity() {
        return CONVERTER.toAuthor(this);
    }

    @Mapper
    interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        Author toAuthor(AuthorInput input);
    }
}
