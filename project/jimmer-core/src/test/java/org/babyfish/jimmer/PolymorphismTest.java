package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class PolymorphismTest {

    @Test
    public void test() throws JsonProcessingException {
        Topic topic = Objects.createTopic(draft -> {
            draft.setName("I love GraphQL");
            draft.setPinnedComment(
                    new ManualComment(
                            "Mr Alex is the expert of GraphQL",
                            "admin-001"
                    )
            );
            draft.setComments(
                    Arrays.asList(
                            new RobotComment(
                                    "Be rational and polite",
                                    1
                            ),
                            new ManualComment(
                                    "Oh yeah",
                                    "guest-003"
                            )
                    )
            );
        });
        String json = topic.toString();
        Assertions.assertEquals(
                "{\"name\":\"I love GraphQL\"," +
                        "\"pinnedComment\":{\"text\":null}," +
                        "\"comments\":[" +
                        "{\"type\":\"R\",\"text\":\"Be rational and polite\",\"level\":1}," +
                        "{\"type\":\"M\",\"text\":null,\"userId\":\"guest-003\"}" +
                        "]" +
                        "}",
                json
        );
        // TODO
    }
}
