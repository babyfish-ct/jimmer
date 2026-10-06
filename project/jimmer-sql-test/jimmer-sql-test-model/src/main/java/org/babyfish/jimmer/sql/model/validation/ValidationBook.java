package org.babyfish.jimmer.sql.model.validation;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.ForeignKeyType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.JoinTable;
import org.babyfish.jimmer.sql.ManyToMany;
import org.babyfish.jimmer.sql.ManyToOne;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity(microServiceName = "database-validation")
public interface ValidationBook {

    @Id
    long id();

    @ManyToOne
    @JoinColumn(name = "STORE_ID", foreignKeyType = ForeignKeyType.REAL)
    ValidationStore store();

    @ManyToOne
    @JoinColumn(name = "FAKE_STORE_ID", foreignKeyType = ForeignKeyType.FAKE)
    @Nullable
    ValidationStore fakeStore();

    @ManyToOne
    @JoinColumn(name = "IGNORED_STORE_ID", foreignKeyType = ForeignKeyType.REAL)
    @DatabaseValidationIgnore
    ValidationStore ignoredStore();

    @ManyToOne
    @JoinColumn(name = "IGNORED_TARGET_ID", foreignKeyType = ForeignKeyType.REAL)
    ValidationIgnoredStore ignoredTarget();

    @ManyToMany
    @JoinTable(name = "VALIDATION_BOOK_STORE_MAPPING", joinColumnName = "BOOK_ID", inverseJoinColumnName = "STORE_ID")
    List<ValidationStore> stores();
}
