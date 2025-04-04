package org.babyfish.jimmer.sql.model.wild;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.model.hr.ConverterForIssue937;

import java.util.List;

@Entity
@KeyUniqueConstraint
public interface Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @JsonConverter(ConverterForIssue937.class)
    String name();

    @OneToMany(mappedBy = "owner")
    List<Task> tasks();
}
