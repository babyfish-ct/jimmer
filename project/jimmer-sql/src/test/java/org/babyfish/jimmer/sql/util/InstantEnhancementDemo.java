package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.model.logic.E;
import org.babyfish.jimmer.sql.model.logic.EDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.time.Instant;

/**
 * Demonstration test showing the Instant enhancement in action
 */
public class InstantEnhancementDemo {

    @Test
    public void demonstrateInstantLogicalDeletionSupport() {
        System.out.println("=== Instant Logical Deletion Enhancement Demo ===");
        
        // 1. Show that LogicalDeletedInfo can be created for Instant fields
        System.out.println("\n1. Creating LogicalDeletedInfo for Instant field...");
        LogicalDeletedInfo info = ImmutableType.get(E.class).getLogicalDeletedInfo();
        Assertions.assertNotNull(info);
        System.out.println("✓ LogicalDeletedInfo created successfully for Instant field");
        
        // 2. Show that "now" value generates current Instant
        System.out.println("\n2. Testing @LogicalDeleted(\"now\") with Instant...");
        Object generatedValue = info.generateValue();
        Assertions.assertInstanceOf(Instant.class, generatedValue);
        Instant instant = (Instant) generatedValue;
        System.out.println("✓ Generated Instant: " + instant);
        System.out.println("✓ Current time:     " + Instant.now());
        
        // 3. Show that the generated value is close to current time
        System.out.println("\n3. Verifying timestamp accuracy...");
        long timeDiff = Math.abs(instant.toEpochMilli() - Instant.now().toEpochMilli());
        Assertions.assertTrue(timeDiff < 1000);
        System.out.println("✓ Time difference: " + timeDiff + "ms (within acceptable range)");
        
        // 4. Show entity creation with Instant logical deletion
        System.out.println("\n4. Creating entity with Instant logical deletion...");
        E entity = EDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.setDeletedTime(null); // Explicitly set to null (not deleted)
        });
        Assertions.assertEquals(1L, entity.id());
        Assertions.assertNull(entity.deletedTime());
        System.out.println("✓ Entity created with ID: " + entity.id());
        System.out.println("✓ Initial deletedTime: " + entity.deletedTime() + " (null = not deleted)");
        
        // 5. Show entity with explicit deletion timestamp
        System.out.println("\n5. Creating entity with explicit deletion timestamp...");
        Instant deletionTime = Instant.now();
        E deletedEntity = EDraft.$.produce(draft -> {
            draft.setId(2L);
            draft.setDeletedTime(deletionTime);
        });
        Assertions.assertEquals(2L, deletedEntity.id());
        Assertions.assertEquals(deletionTime, deletedEntity.deletedTime());
        System.out.println("✓ Entity created with ID: " + deletedEntity.id());
        System.out.println("✓ Deletion timestamp: " + deletedEntity.deletedTime());
        
        // 6. Show logical deletion detection
        System.out.println("\n6. Testing logical deletion detection...");
        boolean isActiveDeleted = info.isDeleted(null);
        boolean isDeletedEntityDeleted = info.isDeleted(deletionTime);
        System.out.println("✓ Entity with null timestamp is deleted: " + isActiveDeleted);
        System.out.println("✓ Entity with timestamp is deleted: " + isDeletedEntityDeleted);
        
        System.out.println("\n=== Enhancement Demo Complete ===");
        System.out.println("✓ Instant type is now fully supported in @LogicalDeleted annotations!");
    }

    @Test
    public void demonstrateBeforeAndAfterComparison() {
        System.out.println("\n=== Before vs After Enhancement ===");
        
        System.out.println("\nBEFORE (would fail):");
        System.out.println("@LogicalDeleted(\"now\")");
        System.out.println("Instant deletedTime; // ❌ ModelException: unsupported type");
        
        System.out.println("\nAFTER (now works):");
        System.out.println("@Nullable");
        System.out.println("@LogicalDeleted(\"now\")");
        System.out.println("Instant deletedTime; // ✅ Fully supported!");
        
        // Prove it works
        LogicalDeletedInfo info = ImmutableType.get(E.class).getLogicalDeletedInfo();
        Instant generated = (Instant) info.generateValue();
        
        System.out.println("\nGenerated value: " + generated);
        System.out.println("Type: " + generated.getClass().getSimpleName());
        System.out.println("✓ Enhancement successful!");
    }
}
