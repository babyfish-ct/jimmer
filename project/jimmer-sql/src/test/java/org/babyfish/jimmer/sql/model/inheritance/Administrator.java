package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;

@Entity
@StaticType(alias = "default", topLevelName = "TheAdministratorInput")
@StaticType(alias = "declared", topLevelName = "DeclaredAdministratorInput", autoScalarStrategy = AutoScalarStrategy.DECLARED)
public interface Administrator extends AdministratorBase {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    long getId();
}
