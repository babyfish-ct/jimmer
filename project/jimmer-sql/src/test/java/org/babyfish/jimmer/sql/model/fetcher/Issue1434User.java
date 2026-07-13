package org.babyfish.jimmer.sql.model.fetcher;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.JoinTable;
import org.babyfish.jimmer.sql.ManyToMany;
import org.babyfish.jimmer.sql.Table;

import java.util.List;

@Entity
@Table(name = "ISSUE_1434_USER")
public interface Issue1434User {

    @Id
    long id();

    String name();

    @ManyToMany
    @JoinTable(
            name = "ISSUE_1434_USER_DEPARTMENT_MAPPING",
            joinColumnName = "USER_ID",
            inverseJoinColumnName = "DEPARTMENT_ID"
    )
    List<Issue1434Department> departments();
}
