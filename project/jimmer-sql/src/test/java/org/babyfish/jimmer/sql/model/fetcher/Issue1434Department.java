package org.babyfish.jimmer.sql.model.fetcher;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ISSUE_1434_DEPARTMENT")
public interface Issue1434Department {

    @Id
    long id();

    String name();
}
