package org.babyfish.jimmer.sql.model.kw;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "\"group\"")
public interface Group {

    @Id
    @Column(name = "\"column\"")
    long column();
}
