package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.inheritance.Role;
import org.babyfish.jimmer.sql.model.inheritance.dto.RoleWithMappedSuperclassFragmentView;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MappedSuperclassFragmentTest {

    @Test
    public void testMappedSuperclassFragment() {
        LocalDateTime createdTime = LocalDateTime.of(2026, 7, 18, 12, 0);
        LocalDateTime modifiedTime = createdTime.plusHours(1);
        RoleWithMappedSuperclassFragmentView view = new RoleWithMappedSuperclassFragmentView();
        view.setId(1L);
        view.setName("admin");
        view.setCreatedTime(createdTime);
        view.setModifiedTime(modifiedTime);

        Role role = view.toEntity();
        assertEquals(1L, role.getId());
        assertEquals("admin", role.getName());
        assertEquals(createdTime, role.getCreatedTime());
        assertEquals(modifiedTime, role.getModifiedTime());
    }
}
