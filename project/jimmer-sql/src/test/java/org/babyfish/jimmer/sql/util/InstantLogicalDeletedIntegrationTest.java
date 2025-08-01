package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.model.logic.E;
import org.babyfish.jimmer.sql.model.logic.F;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.time.Instant;

/**
 * Integration test to verify that @LogicalDeleted annotation works correctly with Instant types
 * This test verifies that the annotation processing and metadata generation works correctly
 */
public class InstantLogicalDeletedIntegrationTest {

    @Test
    public void testInstantLogicalDeletedMetadataGeneration() {
        // Test that LogicalDeletedInfo can be created for Instant fields without exceptions
        ImmutableType typeE = ImmutableType.get(E.class);
        LogicalDeletedInfo infoE = typeE.getLogicalDeletedInfo();
        
        Assertions.assertNotNull(infoE, "LogicalDeletedInfo should be generated for entity E with Instant field");
        Assertions.assertEquals(Instant.class, infoE.getProp().getReturnClass(),
                "Property return class should be Instant");
        Assertions.assertEquals("deletedTime", infoE.getProp().getName(),
                "Property name should be deletedTime");
    }

    @Test
    public void testInstantLogicalDeletedNullMetadataGeneration() {
        // Test that LogicalDeletedInfo can be created for Instant fields with "null" value
        ImmutableType typeF = ImmutableType.get(F.class);
        LogicalDeletedInfo infoF = typeF.getLogicalDeletedInfo();
        
        Assertions.assertNotNull(infoF, "LogicalDeletedInfo should be generated for entity F with Instant field");
        Assertions.assertEquals(Instant.class, infoF.getProp().getReturnClass(),
                "Property return class should be Instant");
        Assertions.assertEquals("deletedTime", infoF.getProp().getName(),
                "Property name should be deletedTime");
    }

    @Test
    public void testInstantNowValueGeneration() {
        ImmutableType typeE = ImmutableType.get(E.class);
        LogicalDeletedInfo infoE = typeE.getLogicalDeletedInfo();
        
        // Test generateValue() for "now" annotation
        Object generatedValue = infoE.generateValue();
        Assertions.assertInstanceOf(Instant.class, generatedValue,
                "Generated value should be an Instant for @LogicalDeleted(\"now\")");
        
        Instant instant = (Instant) generatedValue;
        Instant now = Instant.now();
        
        // Verify the generated value is close to current time
        long timeDiff = Math.abs(instant.toEpochMilli() - now.toEpochMilli());
        Assertions.assertTrue(timeDiff < 1000,
                "Generated Instant should be within 1 second of current time. Diff: " + timeDiff + "ms");
    }

    @Test
    public void testInstantNullValueGeneration() {
        ImmutableType typeF = ImmutableType.get(F.class);
        LogicalDeletedInfo infoF = typeF.getLogicalDeletedInfo();
        
        // Test generateValue() for "null" annotation
        Object generatedValue = infoF.generateValue();
        Assertions.assertNull(generatedValue,
                "Generated value should be null for @LogicalDeleted(\"null\")");
    }

    @Test
    public void testInstantInitializedValueAllocation() {
        ImmutableType typeE = ImmutableType.get(E.class);
        LogicalDeletedInfo infoE = typeE.getLogicalDeletedInfo();
        
        // Test allocateInitializedValue() for nullable Instant field
        Object initializedValue = infoE.allocateInitializedValue();
        Assertions.assertNull(initializedValue,
                "Initialized value should be null for nullable Instant field");
    }

    @Test
    public void testInstantLogicalDeletionLogic() {
        ImmutableType typeE = ImmutableType.get(E.class);
        LogicalDeletedInfo infoE = typeE.getLogicalDeletedInfo();
        
        // Test isDeleted logic for Instant values
        Instant testTime = Instant.now();
        
        // For nullable fields with "now" value:
        // - null means "not deleted" (entity is active)
        // - non-null means "deleted" (entity is logically deleted)
        boolean isDeletedWithNull = infoE.isDeleted(null);
        boolean isDeletedWithValue = infoE.isDeleted(testTime);

        Assertions.assertFalse(isDeletedWithNull,
                "null Instant should indicate entity is not deleted");
        Assertions.assertTrue(isDeletedWithValue,
                "non-null Instant should indicate entity is deleted");
    }

    @Test
    public void testInstantPropertyMetadata() {
        ImmutableType typeE = ImmutableType.get(E.class);
        LogicalDeletedInfo infoE = typeE.getLogicalDeletedInfo();
        
        // Verify property metadata
        Assertions.assertTrue(infoE.getProp().isNullable(),
                "Instant logical deletion property should be nullable");
        Assertions.assertEquals("deletedTime", infoE.getProp().getName(),
                "Property name should match the field name");
        Assertions.assertEquals(Instant.class, infoE.getProp().getReturnClass(),
                "Property return class should be Instant");
    }

    @Test
    public void testMultipleInstantGeneration() {
        ImmutableType typeE = ImmutableType.get(E.class);
        LogicalDeletedInfo infoE = typeE.getLogicalDeletedInfo();
        
        // Generate multiple values to ensure consistency
        Instant first = (Instant) infoE.generateValue();
        
        // Small delay to ensure different timestamps
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Instant second = (Instant) infoE.generateValue();
        
        Assertions.assertNotNull(first, "First generated Instant should not be null");
        Assertions.assertNotNull(second, "Second generated Instant should not be null");
        
        // Verify both are valid Instants and second is equal or after first
        Assertions.assertTrue(second.toEpochMilli() >= first.toEpochMilli(),
                "Second generated Instant should be equal or after first");
    }
}
