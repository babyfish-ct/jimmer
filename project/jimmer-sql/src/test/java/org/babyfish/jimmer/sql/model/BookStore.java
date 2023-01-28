package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import org.babyfish.jimmer.sql.*;
import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@StaticType(alias = "dto", topLevelName = "BookStoreDto")
@Entity
public interface BookStore {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    @Key
    String name();

    @Null
    String website();

    @Version
    int version();

    @Transient(BookStoreAvgPriceResolver.class)
    BigDecimal avgPrice();

    @OneToMany(mappedBy = "store")
    @Static(alias = "dto", targetAlias = "dto")
    List<Book> books();
}
