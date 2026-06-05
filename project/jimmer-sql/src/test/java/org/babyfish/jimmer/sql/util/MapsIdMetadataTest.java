package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.MappedId;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.mapsid.DualParentChild;
import org.babyfish.jimmer.sql.model.mapsid.MappedTenantDocument;
import org.babyfish.jimmer.sql.model.mapsid.MappedTenantDocumentBase;
import org.babyfish.jimmer.sql.model.mapsid.MappedTenantDocumentIdBase;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdMessageDelivery;
import org.babyfish.jimmer.sql.model.mapsid.LongMapsIdProfile;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdProfile;
import org.babyfish.jimmer.sql.model.mapsid.TenantDocument;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
    public void testWholeScalarIdMapping() {
        ImmutableType type = ImmutableType.get(LongMapsIdProfile.class);
        List<MappedId> mappedIds = type.getMappedIds();

        assertEquals(1, mappedIds.size());
        MappedId mappedId = mappedIds.get(0);
        assertEquals("tenant", mappedId.getProp().getName());
        assertEquals("id", mappedId.getIdProp().getName());
        assertEquals("id", mappedId.getTargetIdProp().getName());
        assertTrue(mappedId.isFull());
        assertEquals(
                "[id->ID]",
                mappedId.getColumns(((JSqlClientImplementor) getSqlClient()).getMetadataStrategy())
                        .stream()
                        .map(it -> it.getSourceName() + "->" + it.getTargetName())
                        .collect(Collectors.toList())
                        .toString()
        );
    }

    @Test
    public void testWholeScalarIdMappingWithAssociationIdName() {
        ImmutableType type = ImmutableType.get(MapsIdMessageDelivery.class);
        List<MappedId> mappedIds = type.getMappedIds();

        assertEquals("messageId", type.getIdProp().getName());
        assertNull(type.getProp("messageId").getIdViewBaseProp());
        assertEquals(1, mappedIds.size());
        MappedId mappedId = mappedIds.get(0);
        assertEquals("message", mappedId.getProp().getName());
        assertEquals("messageId", mappedId.getIdProp().getName());
        assertEquals("id", mappedId.getTargetIdProp().getName());
        assertTrue(mappedId.isFull());
        assertEquals(
                "[MESSAGE_ID->ID]",
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

    @Test
    public void testMappedSuperclassMapping() {
        ImmutableType type = ImmutableType.get(MappedTenantDocument.class);
        List<MappedId> mappedIds = type.getMappedIds();

        assertEquals(1, mappedIds.size());
        MappedId mappedId = mappedIds.get(0);
        assertEquals("tenant", mappedId.getProp().getName());
        assertNotNull(ImmutableType.get(MappedTenantDocumentBase.class).getProp("tenant"));
        assertNotNull(ImmutableType.get(MappedTenantDocumentIdBase.class).getProp("id"));
        assertEquals(MappedTenantDocument.class, mappedId.getProp().getDeclaringType().getJavaClass());
        assertEquals("id", mappedId.getIdProp().getName());
        assertEquals(MappedTenantDocument.class, mappedId.getIdProp().getDeclaringType().getJavaClass());
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

    @Test
    public void testMultiplePartialEmbeddedIdMappings() {
        ImmutableType type = ImmutableType.get(DualParentChild.class);
        List<MappedId> mappedIds = type.getMappedIds();

        assertEquals(2, mappedIds.size());
        Map<String, MappedId> mappedIdMap = mappedIds
                .stream()
                .collect(Collectors.toMap(it -> it.getProp().getName(), it -> it));
        assertEquals("[left, right]", mappedIds.stream().map(it -> it.getProp().getName()).collect(Collectors.toList()).toString());
        assertEquals(
                "[leftId]",
                mappedIdMap.get("left")
                        .getIdPath()
                        .stream()
                        .map(ImmutableProp::getName)
                        .collect(Collectors.toList())
                        .toString()
        );
        assertEquals(
                "[rightId]",
                mappedIdMap.get("right")
                        .getIdPath()
                        .stream()
                        .map(ImmutableProp::getName)
                        .collect(Collectors.toList())
                        .toString()
        );
        assertEquals(
                "[LEFT_ID->ID]",
                mappedIdMap.get("left")
                        .getColumns(((JSqlClientImplementor) getSqlClient()).getMetadataStrategy())
                        .stream()
                        .map(it -> it.getSourceName() + "->" + it.getTargetName())
                        .collect(Collectors.toList())
                        .toString()
        );
        assertEquals(
                "[RIGHT_ID->ID]",
                mappedIdMap.get("right")
                        .getColumns(((JSqlClientImplementor) getSqlClient()).getMetadataStrategy())
                        .stream()
                        .map(it -> it.getSourceName() + "->" + it.getTargetName())
                        .collect(Collectors.toList())
                        .toString()
        );
    }
}
