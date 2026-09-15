package org.babyfish.jimmer.sql.binlog;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.jackson.v3.JsonCodecV3;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogImpl;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogParser;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumClient;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumPerson;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.singletable.Asset;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.singletable.Car;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.singletable.Vehicle;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Client;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Person;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SingleTableBinLogTest {

    private static final String PERSON_ROW = "{\"ID\":101,\"CLIENT_TYPE\":\"Person\",\"NAME\":\"Bob\"," +
            "\"FIRST_NAME\":\"Bob\",\"LAST_NAME\":\"Brown\",\"TAX_CODE\":null}";

    static Stream<JsonCodec<?>> codecs() {
        return Stream.of(new JsonCodecV2(), new JsonCodecV3());
    }

    @ParameterizedTest
    @MethodSource("codecs")
    public void testEntityEvents(JsonCodec<?> codec) throws Exception {
        JSqlClient sqlClient = JSqlClient.newBuilder().setDefaultBinLogJsonCodec(codec).build();
        List<EntityEvent<?>> events = new ArrayList<>();
        sqlClient.getTriggers().addEntityListener(events::add);
        String updatedRow = PERSON_ROW.replace("Bob", "Robert");
        sqlClient.getBinLog().accept("CLIENT", null, codec.treeReader().read(PERSON_ROW), "insert");
        sqlClient.getBinLog().accept("CLIENT", codec.treeReader().read(PERSON_ROW), codec.treeReader().read(updatedRow), "update");
        sqlClient.getBinLog().accept("CLIENT", codec.treeReader().read(updatedRow), null, "delete");
        assertEquals(3, events.size());
        for (EntityEvent<?> event : events) {
            assertEquals(ImmutableType.get(Person.class), event.getImmutableType());
        }
        assertNull(events.get(0).getOldEntity());
        assertEquals("Bob", assertInstanceOf(Person.class, events.get(0).getNewEntity()).firstName());
        assertEquals("Bob", assertInstanceOf(Person.class, events.get(1).getOldEntity()).firstName());
        assertEquals("Robert", assertInstanceOf(Person.class, events.get(1).getNewEntity()).firstName());
        assertEquals("Robert", assertInstanceOf(Person.class, events.get(2).getOldEntity()).firstName());
        assertNull(events.get(2).getNewEntity());
        assertEquals("update", events.get(1).getReason());
    }

    @ParameterizedTest
    @MethodSource("codecs")
    public void testExplicitSubtype(JsonCodec<?> codec) {
        Person person = parser(codec).parseEntity(Person.class, PERSON_ROW);
        assertEquals("Brown", person.lastName());
    }

    @ParameterizedTest
    @MethodSource("codecs")
    public void testCustomDiscriminatorAndQuotedColumns(JsonCodec<?> codec) {
        Client client = parser(codec).parseEntity(Client.class,
                "{\"id\":102,\"name\":\"Acme\",\"[tax_CODE]\":\"TAX\",\"FIRST_NAME\":null," +
                        "\"LAST_NAME\":null,\"`client_TYPE`\":\"ORG\"}");
        assertInstanceOf(Organization.class, client);
        assertEquals("TAX", ((Organization) client).taxCode());
        assertEquals("ORG", client.type());
    }

    @ParameterizedTest
    @MethodSource("codecs")
    public void testUnknownColumns(JsonCodec<?> codec) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                parser(codec).parseEntity(Client.class, PERSON_ROW.replace("\"TAX_CODE\":null", "\"UNKNOWN\":null")));
        assertTrue(rootCause(ex).getMessage().contains("UNKNOWN"));
    }

    @ParameterizedTest
    @MethodSource("codecs")
    public void testInvalidDiscriminator(JsonCodec<?> codec) {
        BinLogParser parser = parser(codec);
        for (String row : new String[] {
                "{\"ID\":101}",
                "{\"ID\":101,\"CLIENT_TYPE\":null}",
                "{\"ID\":101,\"CLIENT_TYPE\":\"UNKNOWN\"}"
        }) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parser.parseEntity(Client.class, row));
            assertTrue(rootCause(ex).getMessage().contains("discriminator"));
        }
        assertThrows(IllegalArgumentException.class, () -> parser.parseEntity(Organization.class, PERSON_ROW));
    }

    @ParameterizedTest
    @MethodSource("codecs")
    public void testMultilevelHierarchy(JsonCodec<?> codec) {
        BinLogParser parser = parser(codec);
        String row = "{\"ID\":1,\"ASSET_TYPE\":\"CAR\",\"NAME\":\"Car\",\"MANUFACTURER\":\"Factory\"," +
                "\"SEAT_COUNT\":4,\"PAYLOAD\":null,\"FORMAT\":null}";
        for (Class<? extends Asset> type : Arrays.asList(Asset.class, Vehicle.class, Car.class)) {
            Asset asset = parser.parseEntity(type, row);
            assertInstanceOf(Car.class, asset);
            assertEquals("Factory", ((Car) asset).manufacturer());
            assertEquals(4, ((Car) asset).seatCount());
        }
    }

    @ParameterizedTest
    @MethodSource("codecs")
    public void testEnumDiscriminatorAndConcreteRoot(JsonCodec<?> codec) {
        BinLogParser parser = parser(codec);
        EnumClient person = parser.parseEntity(EnumClient.class,
                "{\"ID\":1,\"CLIENT_TYPE\":\"PERSON\",\"NAME\":\"Bob\",\"FIRST_NAME\":\"Bob\"}");
        assertInstanceOf(EnumPerson.class, person);
        assertEquals("Bob", ((EnumPerson) person).firstName());
        EnumClient root = parser.parseEntity(EnumClient.class,
                "{\"ID\":2,\"CLIENT_TYPE\":\"CLIENT\",\"NAME\":null,\"FIRST_NAME\":null}");
        assertEquals(ImmutableType.get(EnumClient.class), ((ImmutableSpi) root).__type());
    }

    private static BinLogParser parser(JsonCodec<?> codec) {
        JSqlClient sqlClient = JSqlClient.newBuilder().setDefaultBinLogJsonCodec(codec).build();
        return ((BinLogImpl) sqlClient.getBinLog()).parser();
    }

    private static Throwable rootCause(Throwable ex) {
        while (ex.getCause() != null) {
            ex = ex.getCause();
        }
        return ex;
    }
}
