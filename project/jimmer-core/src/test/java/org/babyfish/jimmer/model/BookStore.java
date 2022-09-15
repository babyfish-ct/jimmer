package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.*;
import java.math.BigDecimal;
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

    @Null
    @PositiveOrZero
    BigDecimal avgPrice();
}
