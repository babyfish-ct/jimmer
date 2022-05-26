package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.sql.Key;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

@Entity
public interface BookStore {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sequence:BOOK_STORE_ID_SEQ"
    )
    long id();

    @Key
    @NotBlank
    String name();

    @Nullable
    @NotBlank
    String website();

    @Transient
    BigDecimal avgPrice();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}