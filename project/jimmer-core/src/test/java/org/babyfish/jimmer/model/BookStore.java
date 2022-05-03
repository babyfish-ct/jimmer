package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Immutable
public interface BookStore {

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "[^\\d]+\\S+")
    String name();

    @Null
    String website();

    List<Book> books();
}
