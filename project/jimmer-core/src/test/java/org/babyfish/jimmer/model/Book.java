package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.util.List;

@Immutable
public interface Book {

    @NotBlank
    @Size(min = 1, max = 50)
    String name();

    @Null
    BookStore store();

    // use "int" to test primitive type
    @Positive
    int price();

    List<Author> authors();
}
