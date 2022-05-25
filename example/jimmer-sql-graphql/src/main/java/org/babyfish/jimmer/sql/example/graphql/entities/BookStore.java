package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
public interface BookStore {

    @Id
    @GeneratedValue(generator = UUIDIdGenerator.FULL_NAME)
    UUID id();

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