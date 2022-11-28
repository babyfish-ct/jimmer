package org.babyfish.jimmer.sql.example.graphql.entities.input;

import org.babyfish.jimmer.ImmutableConverter;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorDraft;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorProps;
import org.babyfish.jimmer.sql.example.graphql.entities.Gender;
import org.springframework.lang.Nullable;

public class AuthorInput {

    private static final ImmutableConverter<Author, AuthorInput> CONVERTER =
            ImmutableConverter.
                    newBuilder(Author.class, AuthorInput.class)
                    .map(AuthorProps.ID, mapping -> {
                        mapping.useIf(input -> input.id != null);
                    })
                    .autoMapOtherScalars(true)
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

    public Author toAuthor() {
        return AuthorDraft.$.produce(author -> {
            if (id != null) {
                author.setId(id);
            }
            author
                    .setFirstName(firstName)
                    .setLastName(lastName)
                    .setGender(gender);
        });
    }
}
