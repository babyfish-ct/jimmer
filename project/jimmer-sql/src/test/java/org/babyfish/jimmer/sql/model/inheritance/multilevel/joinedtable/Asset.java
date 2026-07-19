package org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "ML_JOINED_ASSET")
@Inheritance(strategy = InheritanceType.JOINED)
public interface Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Discriminator
    @Column(name = "ASSET_TYPE")
    String type();

    String name();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "MANAGER_ID")
    VehicleOwner manager();
}
