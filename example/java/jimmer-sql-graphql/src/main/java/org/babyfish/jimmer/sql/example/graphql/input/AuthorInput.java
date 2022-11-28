package org.babyfish.jimmer.sql.example.graphql.entities.input;

import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorDraft;
import org.babyfish.jimmer.sql.example.graphql.entities.Gender;
import org.springframework.lang.Nullable;

public class AuthorInput {

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
