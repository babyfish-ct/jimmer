package org.babyfish.jimmer.sql.model.inheritance.multilevel.singletable;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Discriminator;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Inheritance;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ML_SINGLE_ASSET")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public interface Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Discriminator
    @Column(name = "ASSET_TYPE")
    String type();

    String name();
}
