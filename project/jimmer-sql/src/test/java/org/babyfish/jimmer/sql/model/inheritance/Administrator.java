package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface Administrator extends AdministratorBase, UserInfo {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    long getId();
}
