package org.babyfish.jimmer.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Immutable
public interface BookStore {

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "[^\\d]+\\S+")
    @UpperCase
    String name();

    @Null
    String website();

    List<Book> books();

    @Null
    @PositiveOrZero
    BigDecimal avgPrice();
}
