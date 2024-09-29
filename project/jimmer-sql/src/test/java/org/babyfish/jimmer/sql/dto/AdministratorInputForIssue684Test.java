package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.inheritance.dto.AdministratorInputForIssue684;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdministratorInputForIssue684Test {

    @Test
    public void testNull() {
        AdministratorInputForIssue684 input = new AdministratorInputForIssue684();
        Assertions.assertEquals(
                "{}",
                input.toEntity().toString()
        );
    }

    @Test
    public void testNonNull() {
        AdministratorInputForIssue684 input = new AdministratorInputForIssue684();
        AdministratorInputForIssue684.TargetOf_metadata metadata = new AdministratorInputForIssue684.TargetOf_metadata();
        metadata.setName("Metadata");
        input.setMetadata(metadata);
        Assertions.assertEquals(
                "{\"metadata\":{\"name\":\"Metadata\"}}",
                input.toEntity().toString()
        );
    }
}
