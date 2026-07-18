package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.codec.JsonCodec;
import org.babyfish.jimmer.json.codec.Node;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Jackson3BinaryCompatibilityTest {

    @Test
    public void oldPublicPackageNamesStillWork() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "jimmer");
        map.put("score", 7);
        String json = "{\"name\":\"jimmer\",\"score\":7}";

        JsonCodec codec = new JsonCodecV3(mapper);
        assertEquals(json, codec.writer().writeAsString(map));
        assertEquals("jimmer", codec.treeReader().read(json).get("name").castTo(String.class));

        JsonCodecProviderV3 provider = new JsonCodecProviderV3();
        assertEquals(300, provider.priority());
        assertTrue(provider.codec() instanceof JsonCodec);
        assertEquals(map, new JsonConverterV3(mapper).convert(map, Map.class));
        assertEquals(map, new JsonReaderV3<Map<String, Object>>(mapper.readerFor(Map.class)).read(json));
        assertEquals(json, new JsonWriterV3(mapper.writer()).writeAsString(map));

        Node node = new NodeV3(mapper.readTree(json));
        assertEquals(7, node.get("score").castTo(int.class));
        assertTrue(new NodePropertiesIteratorV3(mapper.readTree(json).properties().iterator()).hasNext());

        ModulesRegistrarV3.registerImmutableModule(JsonMapper.builder());
        ModulesRegistrarV3.registerWellKnownModules(JsonMapper.builder());
        assertEquals(ImmutableModuleV3.MODULE_ID, new ImmutableModuleV3().getRegistrationId());
    }
}
