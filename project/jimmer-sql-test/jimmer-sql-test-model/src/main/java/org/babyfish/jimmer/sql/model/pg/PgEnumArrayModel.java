package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.arrays.Priority;

import java.util.List;

@DatabaseValidationIgnore
@Entity
public interface PgEnumArrayModel {

    @Id
    long id();

    @Column(sqlElementType = "varchar")
    Gender[] genderArr();

    List<Gender> textList();

    @Column(sqlElementType = "varchar")
    List<Gender> varcharList();

    List<Priority> priorities();
}
