package org.babyfish.jimmer.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ManualComment.class, name = "M"),
        @JsonSubTypes.Type(value = RobotComment.class, name = "R")
})
public interface Comment {

    String getText();
}
