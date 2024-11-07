package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.babyfish.jimmer.model.Hospital;
import org.babyfish.jimmer.model.Immutables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Issue748Test {

    @Test
    public void test() throws JsonProcessingException {
        Hospital hospital = Immutables.createHospital(it -> it.setName("XieHe").setISPublic(true));
        String json = hospital.toString();
        Assertions.assertEquals("{\"name\":\"XieHe\",\"ispublic\":true}", json);
        Hospital hospital2 = ImmutableObjects.fromString(Hospital.class, json);
        Assertions.assertEquals(
                "{\"name\":\"XieHe\",\"ispublic\":true}",
                hospital2.toString()
        );
    }
}
