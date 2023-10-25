package org.babyfish.jimmer.sql.model.enumeration;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@DatabaseValidationIgnore
@Entity
public interface Article {

    @Id
    @GeneratedValue
    long id();

    String name();

    @ManyToOne
    Writer writer();

    @ManyToOne
    Approver approver();
}
