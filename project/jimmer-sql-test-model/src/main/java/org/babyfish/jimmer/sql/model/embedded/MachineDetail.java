package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Serialized;

import java.util.Map;

@Embeddable
public interface MachineDetail {

    @Column(name = "factory_map")
    @Serialized
    Map<String, String> factories();

    @Column(name = "patent_map")
    @Serialized
    Map<String, String> patents();
}
