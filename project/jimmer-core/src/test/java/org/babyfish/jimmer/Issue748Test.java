package org.babyfish.jimmer;

import org.babyfish.jimmer.model.Hospital;
import org.babyfish.jimmer.model.Immutables;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import static org.babyfish.jimmer.support.JsonAssertions.assertJsonEquals;

public class Issue748Test {
    @JacksonTest
    public void test(JacksonBackend backend) throws Exception {
        Hospital hospital = Immutables.createHospital(it -> it.setName("XieHe"));
        String json = backend.write(hospital);
        assertJsonEquals(backend.codec, "{\"name\":\"XieHe\"}", json);

        Hospital hospital2 = backend.read(Hospital.class, json);
        assertJsonEquals(backend.codec, "{\"name\":\"XieHe\"}", backend.write(hospital2));
    }
}
