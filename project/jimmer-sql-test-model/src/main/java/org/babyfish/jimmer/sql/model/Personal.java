package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongToStringConverter;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import java.util.UUID;

@Entity
@KeyUniqueConstraint
public interface Personal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonConverter(LongToStringConverter.class)
    long id();

    @JsonConverter(PersonalPhoneConverter.class)
    String phone();
}
