package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@DatabaseValidationIgnore
@Entity
public interface PgArrayModel {

    @Id
    long id();

    int[] intArr();

    Integer[] integerArr();

    @Column(sqlElementType = "text")
    String[] textArr();

    @Column(sqlElementType = "text")
    List<String> textList();

    @Column(sqlElementType = "varchar")
    String[] varcharArr();

    @Column(sqlElementType = "varchar")
    List<String> varcharList();
}
