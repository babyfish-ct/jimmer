package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.model.Hospital;
import org.babyfish.jimmer.model.Immutables;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.support.JsonAssertions.assertJsonEquals;

public class Issue748Test {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void test() throws JsonProcessingException {
        Hospital hospital = Immutables.createHospital(it -> it.setName("XieHe").setISPublic(true));
        String json = hospital.toString();
        assertJsonEquals("{\"name\":\"XieHe\",\"ispublic\":true}", json);

        Hospital hospital2 = ImmutableObjects.fromString(Hospital.class, json);
        assertJsonEquals("{\"name\":\"XieHe\",\"ispublic\":true}", hospital2.toString());
    }
}
