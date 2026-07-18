package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.babyfish.jimmer.json.codec.JsonCodec;
import org.babyfish.jimmer.json.codec.Node;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Jackson2BinaryCompatibilityTest {

    @Test
    public void oldPublicPackageNamesStillWork() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "jimmer");
        map.put("score", 7);
        String json = "{\"name\":\"jimmer\",\"score\":7}";

        JsonCodec codec = new JsonCodecV2(mapper);
        assertEquals(json, codec.writer().writeAsString(map));
        assertEquals("jimmer", codec.treeReader().read(json).get("name").castTo(String.class));

        JsonCodecProviderV2 provider = new JsonCodecProviderV2();
        assertEquals(200, provider.priority());
        assertTrue(provider.codec() instanceof JsonCodec);
        assertEquals(map, new JsonConverterV2(mapper).convert(map, Map.class));
        assertEquals(map, new JsonReaderV2<Map<String, Object>>(mapper.readerFor(Map.class)).read(json));
        assertEquals(json, new JsonWriterV2(mapper.writer()).writeAsString(map));

        Node node = new NodeV2(mapper.readTree(json));
        assertEquals(7, node.get("score").castTo(int.class));
        assertTrue(new NodePropertiesIteratorV2(mapper.readTree(json).fields()).hasNext());

        ModulesRegistrarV2.registerImmutableModule(JsonMapper.builder());
        ModulesRegistrarV2.registerWellKnownModules(JsonMapper.builder());
        assertEquals(ImmutableModuleV2.MODULE_ID, new ImmutableModuleV2().getTypeId());
    }
}
