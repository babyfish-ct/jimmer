package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.*;

import java.util.List;
import java.util.Map;

@DatabaseValidationIgnore
@Entity
@Table(name = "pg_json_wrapper")
public interface JsonWrapper {

    @Id
    long id();

    @Column(name = "json_1")
    Point point();

    @Column(name = "json_2")
    List<String> tags();

    @Column(name = "json_3")
    Map<Long, Integer> scores();

    @Column(name = "json_4")
    @Serialized
    List<List<String>> complexList();

    @Column(name = "json_5")
    @Serialized
    Map<String, Map<String, String>> complexMap();
}
