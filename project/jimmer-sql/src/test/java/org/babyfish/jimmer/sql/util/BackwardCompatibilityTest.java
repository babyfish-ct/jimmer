package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.model.logic.A;
import org.babyfish.jimmer.sql.model.logic.B;
import org.babyfish.jimmer.sql.model.logic.C;
import org.babyfish.jimmer.sql.model.logic.D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Test to ensure that adding Instant support doesn't break existing functionality
 */
public class BackwardCompatibilityTest {

    @Test
    public void testExistingIntegerLogicalDeletion() {
        // Test that existing integer logical deletion still works (entity A)
        LogicalDeletedInfo info = ImmutableType.get(A.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity A");
        Assertions.assertEquals(1, info.generateValue(), "Entity A should generate value 1");
    }

    @Test
    public void testExistingEnumLogicalDeletion() {
        // Test that existing enum logical deletion still works (entity B)
        LogicalDeletedInfo info = ImmutableType.get(B.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity B");
        Assertions.assertEquals(B.Status.DISABLED, info.generateValue(), "Entity B should generate DISABLED status");
    }

    @Test
    public void testExistingLocalDateTimeLogicalDeletion() {
        // Test that existing LocalDateTime logical deletion still works (entity C)
        LogicalDeletedInfo info = ImmutableType.get(C.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity C");
        
        Object generatedValue = info.generateValue();
        Assertions.assertInstanceOf(LocalDateTime.class, generatedValue, "Entity C should generate LocalDateTime");
        
        LocalDateTime localDateTime = (LocalDateTime) generatedValue;
        LocalDateTime now = LocalDateTime.now();
        
        // Verify the generated value is close to current time
        long timeDiff = Math.abs(localDateTime.toEpochSecond(ZoneOffset.UTC) - now.toEpochSecond(ZoneOffset.UTC));
        Assertions.assertTrue(timeDiff < 1000, "Generated LocalDateTime should be close to current time");
    }

    @Test
    public void testExistingNullLogicalDeletion() {
        // Test that existing null logical deletion still works (entity D)
        LogicalDeletedInfo info = ImmutableType.get(D.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info, "LogicalDeletedInfo should not be null for entity D");
        Assertions.assertNull(info.generateValue(), "Entity D should generate null value");
    }

    @Test
    public void testAllExistingEntitiesStillWork() {
        // Comprehensive test to ensure all existing entities can still create LogicalDeletedInfo
        Class<?>[] existingEntities = {A.class, B.class, C.class, D.class};
        
        for (Class<?> entityClass : existingEntities) {
            try {
                ImmutableType type = ImmutableType.get(entityClass);
                LogicalDeletedInfo info = type.getLogicalDeletedInfo();
                Assertions.assertNotNull(info, 
                        "LogicalDeletedInfo should not be null for " + entityClass.getSimpleName());
                
                // Verify that generateValue() doesn't throw exceptions
                Object value = info.generateValue();
                // Value can be null for some entities, so we just verify no exception is thrown
                
            } catch (Exception e) {
                Assertions.fail("Entity " + entityClass.getSimpleName() + 
                        " should not throw exception when creating LogicalDeletedInfo: " + e.getMessage());
            }
        }
    }

    @Test
    public void testExistingInitializedValues() {
        // Test that existing initialized value allocation still works
        LogicalDeletedInfo infoA = ImmutableType.get(A.class).getLogicalDeletedInfo();
        LogicalDeletedInfo infoC = ImmutableType.get(C.class).getLogicalDeletedInfo();
        LogicalDeletedInfo infoD = ImmutableType.get(D.class).getLogicalDeletedInfo();
        
        // Entity A (non-nullable int) should have initialized value
        Assertions.assertNotNull(infoA, "LogicalDeletedInfo should not be null for entity A");
        Object initializedA = infoA.allocateInitializedValue();
        Assertions.assertEquals(0, initializedA, "Entity A should have initialized value 0");
        
        // Entity C (nullable LocalDateTime) should have null initialized value
        Assertions.assertNotNull(infoC, "LogicalDeletedInfo should not be null for entity C");
        Object initializedC = infoC.allocateInitializedValue();
        Assertions.assertNull(initializedC, "Entity C should have null initialized value");
        
        // Entity D (nullable LocalDateTime with null) should have null initialized value
        Assertions.assertNotNull(infoD, "LogicalDeletedInfo should not be null for entity D");
        Object initializedD = infoD.allocateInitializedValue();
        Assertions.assertNull(initializedD, "Entity D should have null initialized value");
    }
}
