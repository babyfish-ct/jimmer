package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Immutable
public interface Author {

    @NotBlank
    @Size(min = 1, max = 50)
    String name();

    List<Book> books();

    @Email
    String email();
}
