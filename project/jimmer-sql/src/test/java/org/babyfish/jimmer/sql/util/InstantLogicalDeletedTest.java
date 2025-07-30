package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.model.logic.E;
import org.babyfish.jimmer.sql.model.logic.EDraft;
import org.babyfish.jimmer.sql.model.logic.F;
import org.babyfish.jimmer.sql.model.logic.FDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Comprehensive test for Instant type support in @LogicalDeleted annotation
 */
public class InstantLogicalDeletedTest {

    @Test
    public void testInstantNowSupport() {
        LogicalDeletedInfo info = ImmutableType.get(E.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity E");
        
        // Test that generateValue() returns an Instant close to current time
        Object generatedValue = info.generateValue();
        Assertions.assertInstanceOf(Instant.class, generatedValue, "Generated value should be an Instant");
        
        Instant instant = (Instant) generatedValue;
        Instant now = Instant.now();
        
        // Verify the generated value is within 1 second of current time
        long timeDiff = Math.abs(instant.toEpochMilli() - now.toEpochMilli());
        Assertions.assertTrue(timeDiff < 1000, 
                "Generated Instant should be within 1 second of current time. Diff: " + timeDiff + "ms");
    }

    @Test
    public void testInstantNullSupport() {
        LogicalDeletedInfo info = ImmutableType.get(F.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity F");
        
        // Test that generateValue() returns null for "null" value
        Object generatedValue = info.generateValue();
        Assertions.assertNull(generatedValue, "Generated value should be null for @LogicalDeleted(\"null\")");
    }

    @Test
    public void testInstantInitializedValue() {
        LogicalDeletedInfo info = ImmutableType.get(E.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity E");
        
        // Test that allocateInitializedValue() returns null for nullable field
        Object initializedValue = info.allocateInitializedValue();
        Assertions.assertNull(initializedValue, "Initialized value should be null for nullable Instant field");
    }

    @Test
    public void testEntityDraftWithInstant() {
        // Test creating entity draft with Instant logical deletion field
        E entity = EDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.setDeletedTime(null); // Explicitly set to null (not deleted)
        });

        Assertions.assertEquals(1L, entity.id());
        Assertions.assertNull(entity.deletedTime(), "deletedTime should be null initially");
    }

    @Test
    public void testEntityDraftWithInstantSet() {
        Instant testTime = Instant.now();
        
        // Test creating entity draft with explicitly set Instant
        E entity = EDraft.$.produce(draft -> {
            draft.setId(2L);
            draft.setDeletedTime(testTime);
        });
        
        Assertions.assertEquals(2L, entity.id());
        Assertions.assertEquals(testTime, entity.deletedTime(), "deletedTime should match the set value");
    }

    @Test
    public void testLogicalDeletedAction() {
        LogicalDeletedInfo info = ImmutableType.get(E.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity E");
        
        // Test that isDeleted works correctly with Instant values
        Instant deletedTime = Instant.now();
        
        // Non-null Instant should be considered deleted (for nullable fields with "now" value)
        boolean isDeleted = info.isDeleted(deletedTime);
        Assertions.assertTrue(isDeleted, "Non-null Instant should indicate deleted for nullable field");

        // Null should indicate not deleted
        boolean isNotDeleted = info.isDeleted(null);
        Assertions.assertFalse(isNotDeleted, "Null Instant should indicate not deleted for nullable field");
    }

    @Test
    public void testMultipleInstantGeneration() {
        LogicalDeletedInfo info = ImmutableType.get(E.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity E");
        
        // Generate multiple values and ensure they're all valid Instants
        Instant first = (Instant) info.generateValue();
        
        // Small delay to ensure different timestamps
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Instant second = (Instant) info.generateValue();
        
        Assertions.assertNotNull(first, "First generated Instant should not be null");
        Assertions.assertNotNull(second, "Second generated Instant should not be null");
        
        // Second should be equal or after first (allowing for same millisecond)
        Assertions.assertTrue(second.toEpochMilli() >= first.toEpochMilli(),
                "Second generated Instant should be equal or after first");
    }

    @Test
    public void testInstantTypeValidation() {
        // Verify that the LogicalDeletedInfo recognizes Instant as a valid time type
        LogicalDeletedInfo info = ImmutableType.get(E.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity E");
        
        // The fact that we can get LogicalDeletedInfo without exception means
        // Instant is now properly supported in NOW_SUPPLIER_MAP
        Assertions.assertEquals(Instant.class, info.getProp().getReturnClass(),
                "Property return class should be Instant");
    }
}
