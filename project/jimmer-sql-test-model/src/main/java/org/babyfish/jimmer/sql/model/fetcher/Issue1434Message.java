package org.babyfish.jimmer.sql.model.fetcher;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.ForeignKeyType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.Table;
import org.babyfish.jimmer.sql.Transient;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "ISSUE_1434_MESSAGE")
public interface Issue1434Message {

    @Id
    long id();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "USER_ID", foreignKeyType = ForeignKeyType.FAKE)
    Issue1434User user();

    @Transient(Issue1434MessageUserDepartmentNamesResolver.class)
    String userDepartmentNames();
}
