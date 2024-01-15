package org.babyfish.jimmer.sql.model.arrays;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import java.util.UUID;

/**
 * Testing different array types
 */
@Entity
public interface ArrayModel {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    String[] strings();

    Byte[] bytes();

    Integer[] integers();

    Long [] longs();

    UUID[] uuids();

    Float[] floats();
}
