package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;
import java.lang.reflect.Field;

/**
 * Test to verify that Instant is properly supported in NOW_SUPPLIER_MAP
 */
public class InstantSupportVerificationTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testInstantInNowSupplierMap() throws Exception {
        // Use reflection to access the private NOW_SUPPLIER_MAP
        Field field = LogicalDeletedInfo.class.getDeclaredField("NOW_SUPPLIER_MAP");
        field.setAccessible(true);
        Map<Class<?>, Supplier<Object>> nowSupplierMap = (Map<Class<?>, Supplier<Object>>) field.get(null);
        
        // Verify that Instant.class is in the map
        Assertions.assertTrue(nowSupplierMap.containsKey(Instant.class), 
                "NOW_SUPPLIER_MAP should contain Instant.class");
        
        // Verify that the supplier works correctly
        Supplier<Object> instantSupplier = nowSupplierMap.get(Instant.class);
        Assertions.assertNotNull(instantSupplier, "Instant supplier should not be null");
        
        Object result = instantSupplier.get();
        Assertions.assertInstanceOf(Instant.class, result, "Supplier should return an Instant");
        
        Instant instant = (Instant) result;
        Instant now = Instant.now();
        
        // Verify the generated instant is close to current time (within 1 second)
        long timeDiff = Math.abs(instant.toEpochMilli() - now.toEpochMilli());
        Assertions.assertTrue(timeDiff < 1000, 
                "Generated Instant should be within 1 second of current time. Diff: " + timeDiff + "ms");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllExpectedTypesInNowSupplierMap() throws Exception {
        // Use reflection to access the private NOW_SUPPLIER_MAP
        Field field = LogicalDeletedInfo.class.getDeclaredField("NOW_SUPPLIER_MAP");
        field.setAccessible(true);
        Map<Class<?>, Supplier<Object>> nowSupplierMap = (Map<Class<?>, Supplier<Object>>) field.get(null);
        
        // Verify all expected time types are present
        Class<?>[] expectedTypes = {
            java.util.Date.class,
            java.sql.Date.class,
            java.sql.Time.class,
            java.sql.Timestamp.class,
            java.time.LocalDateTime.class,
            java.time.LocalDate.class,
            java.time.LocalTime.class,
            java.time.OffsetDateTime.class,
            java.time.ZonedDateTime.class,
            java.time.Instant.class  // This is our new addition
        };
        
        for (Class<?> expectedType : expectedTypes) {
            Assertions.assertTrue(nowSupplierMap.containsKey(expectedType), 
                    "NOW_SUPPLIER_MAP should contain " + expectedType.getSimpleName());
        }
        
        // Verify the map has exactly the expected number of entries
        Assertions.assertEquals(expectedTypes.length, nowSupplierMap.size(),
                "NOW_SUPPLIER_MAP should have exactly " + expectedTypes.length + " entries");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInstantSupplierConsistency() throws Exception {
        // Use reflection to access the private NOW_SUPPLIER_MAP
        Field field = LogicalDeletedInfo.class.getDeclaredField("NOW_SUPPLIER_MAP");
        field.setAccessible(true);
        Map<Class<?>, Supplier<Object>> nowSupplierMap = (Map<Class<?>, Supplier<Object>>) field.get(null);
        
        Supplier<Object> instantSupplier = nowSupplierMap.get(Instant.class);
        
        // Generate multiple values and verify they're all valid and increasing
        Instant first = (Instant) instantSupplier.get();
        
        // Small delay to ensure different timestamps
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Instant second = (Instant) instantSupplier.get();
        
        Assertions.assertNotNull(first, "First generated Instant should not be null");
        Assertions.assertNotNull(second, "Second generated Instant should not be null");
        
        // Second should be equal or after first (allowing for same millisecond)
        Assertions.assertTrue(second.toEpochMilli() >= first.toEpochMilli(),
                "Second generated Instant should be equal or after first");
    }
}
