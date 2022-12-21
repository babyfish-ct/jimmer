package org.babyfish.jimmer.sql.example.graphql.entities.input;

import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.spring.model.Input;
import org.babyfish.jimmer.sql.example.graphql.entities.*;
import org.springframework.lang.Nullable;

public class AuthorInput implements Input<Author> {

    private static final ImmutableConverter<Author, AuthorInput> CONVERTER =
            ImmutableConverter.
                    forFields(Author.class, AuthorInput.class)
                    .map(AuthorProps.ID, mapping -> {
                        mapping.useIf(input -> input.id != null);
                    })
                    .build();

    @Nullable
    private final Long id;

    private final String firstName;

    private final String lastName;

    private final Gender gender;

    public AuthorInput(
            @Nullable Long id,
            String firstName,
            String lastName,
            Gender gender
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
    }

    @Override
    public Author toEntity() {
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
        return "AuthorInput{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", gender=" + gender +
                '}';
    }
}
