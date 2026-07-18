package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.json.JsonConverter;
import org.babyfish.jimmer.json.LongToStringConverter;
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
