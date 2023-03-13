package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "pg_json_wrapper")
public interface JsonWrapper {

    @Id
    long id();

    @Nullable
    @Column(name = "json_1")
    Point point();

    @Nullable
    @Column(name = "json_2")
    List<String> tags();

    @Nullable
    @Column(name = "json_3")
    Map<Long, Integer> scores();
}
