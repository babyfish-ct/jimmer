package org.babyfish.jimmer.sql.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.json.JsonWriter;
import org.babyfish.jimmer.json.gson.GsonCodec;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogImpl;
import org.babyfish.jimmer.sql.model.pg.JsonDefaultRow;
import org.babyfish.jimmer.sql.model.pg.JsonDefaultRowDraft;
import org.babyfish.jimmer.sql.model.pg.JsonWrapper;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SqlClientCodecTest {

    @Test
    public void jsonDefaultUsesTheOwningClient() throws Exception {
        ImmutableProp tags = ImmutableType.get(JsonDefaultRow.class).getProp("tags");
        assertEquals(Collections.singletonList("item"), tags.getDefaultValueRef().getValue());
        JSqlClient first = JSqlClient.newBuilder().setJsonCodec(prefixReader("first:")).build();
        JSqlClient second = JSqlClient.newBuilder()
                .setJsonCodec(prefixReader("app:"))
                .setSerializedPropJsonCodec(tags, prefixReader("second:"))
                .build();
        try (Connection con = DriverManager.getConnection("jdbc:h2:mem:");
             Statement statement = con.createStatement()) {
            statement.execute("create table JSON_DEFAULT_ROW (ID bigint primary key, NAME varchar(100), TAGS varchar(100))");
            saveWithDefault(first, con, 1);
            saveWithDefault(second, con, 2);
            saveWithDefault(first, con, 3);
            try (ResultSet rows = statement.executeQuery("select TAGS from JSON_DEFAULT_ROW order by ID")) {
                for (String expected : new String[]{"first:item", "second:item", "first:item"}) {
                    assertTrue(rows.next());
                    assertEquals("[\"" + expected + "\"]", rows.getString(1));
                }
                assertFalse(rows.next());
            }
        }
    }

    private static GsonCodec prefixReader(String prefix) {
        JsonDeserializer<String> reader = (json, type, context) -> prefix + json.getAsString();
        return new GsonCodec(new GsonBuilder().registerTypeAdapter(String.class, reader));
    }

    private static void saveWithDefault(JSqlClient client, Connection con, long id) {
        JsonDefaultRow row = JsonDefaultRowDraft.$.produce(draft -> draft.setId(id).setName("row"));
        client.getEntities().saveCommand(row).setMode(SaveMode.INSERT_ONLY).execute(con);
    }

    @Test
    public void binlogReadsSerializedColumnsFromDocumentsAndDatabaseText() {
        JSqlClient sqlClient = JSqlClient.newBuilder()
                .setJsonCodec(new GsonCodec())
                .setDialect(new PostgresDialect())
                .build();
        String record = "{\"ID\":1,\"JSON_4\":[[\"hello\"]],\"JSON_5\":\"{\\\"one\\\":{\\\"two\\\":\\\"value\\\"}}\"}";
        JsonWrapper row = ((BinLogImpl) sqlClient.getBinLog()).parser().parseEntity(JsonWrapper.class, record);
        assertEquals(Collections.singletonList(Collections.singletonList("hello")), row.complexList());
        assertEquals(Collections.singletonMap("one", Collections.singletonMap("two", "value")), row.complexMap());
    }

    @Test
    public void serializedGenericAndCacheFactoryUseTheOwningClient() throws Exception {
        ImmutableProp prop = ImmutableType.get(JsonWrapper.class).getProp("complexList");
        RecordingCodec first = new RecordingCodec("first:");
        RecordingCodec second = new RecordingCodec("second:");
        JSqlClientImplementor firstClient = client(first);
        JSqlClientImplementor secondClient = client(second);
        List<List<String>> value = Collections.singletonList(Collections.singletonList("item"));
        ScalarProvider<List<List<String>>, String> provider = firstClient.getScalarProvider(prop);
        assertEquals(value, provider.toScalar("[[\"item\"]]"));
        assertEquals("[[\"first:item\"]]", provider.toSql(value));
        assertEquals("[[\"second:item\"]]", secondClient.<List<List<String>>, String>getScalarProvider(prop).toSql(value));
        assertTrue(first.writeTypes.contains(prop.getGenericType()));
        assertTrue(second.writeTypes.contains(prop.getGenericType()));
    }

    private static JSqlClientImplementor client(RecordingCodec codec) {
        return (JSqlClientImplementor) JSqlClient.newBuilder()
                .setCacheFactoryProvider(resolved -> {
                    assertSame(codec, resolved);
                    return new CacheFactory() {};
                })
                .setJsonCodec(codec)
                .build();
    }

    private static class RecordingCodec extends JsonCodecV2 {

        private final Set<Type> writeTypes = new HashSet<>();

        RecordingCodec(String prefix) {
            super(new ObjectMapper().registerModule(new SimpleModule().addSerializer(String.class, new JsonSerializer<String>() {
                @Override
                public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    gen.writeString(prefix + value);
                }
            })));
        }

        @Override
        public <T> JsonWriter<T> writerFor(Type type) {
            writeTypes.add(type);
            return super.writerFor(type);
        }
    }
}
