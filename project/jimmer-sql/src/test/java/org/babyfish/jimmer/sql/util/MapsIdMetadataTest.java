package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.MappedId;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdProfile;
import org.babyfish.jimmer.sql.model.mapsid.TenantDocument;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class MapsIdMetadataTest extends AbstractQueryTest {

    @Test
    public void testWholeEmbeddedIdMapping() {
        ImmutableType type = ImmutableType.get(MapsIdProfile.class);
        List<MappedId> mappedIds = type.getMappedIds();

        assertEquals(1, mappedIds.size());
        MappedId mappedId = mappedIds.get(0);
        assertEquals("principal", mappedId.getProp().getName());
        assertTrue(mappedId.isFull());
        assertEquals(
                "[A->A, B->B]",
                mappedId.getColumns(((JSqlClientImplementor) getSqlClient()).getMetadataStrategy())
                        .stream()
                        .map(it -> it.getSourceName() + "->" + it.getTargetName())
                        .collect(Collectors.toList())
                        .toString()
        );
    }

    @Test
    public void testPartialEmbeddedIdMapping() {
        ImmutableType type = ImmutableType.get(TenantDocument.class);
        List<MappedId> mappedIds = type.getMappedIds();

        assertEquals(1, mappedIds.size());
        MappedId mappedId = mappedIds.get(0);
        assertEquals("tenant", mappedId.getProp().getName());
        assertFalse(mappedId.isFull());
        assertEquals(
                "[tenantId]",
                mappedId.getIdPath().stream().map(ImmutableProp::getName).collect(Collectors.toList()).toString()
        );
        assertEquals(
                "[TENANT_ID->ID]",
                mappedId.getColumns(((JSqlClientImplementor) getSqlClient()).getMetadataStrategy())
                        .stream()
                        .map(it -> it.getSourceName() + "->" + it.getTargetName())
                        .collect(Collectors.toList())
                        .toString()
        );
    }
}
