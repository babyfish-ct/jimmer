package org.babyfish.jimmer.spring.cloud;

import org.babyfish.jimmer.json.gson.GsonCodec;
import org.babyfish.jimmer.spring.java.model.issue1162.Organization;
import org.babyfish.jimmer.spring.java.model.issue1162.OrganizationFetcher;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ExporterCodecTest {

    @Test
    public void testReadAndWriteThroughClientCodec() throws Exception {
        UUID id = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:exporter_codec");
             Statement statement = connection.createStatement()) {
            statement.execute("create table organization(id uuid primary key, idm_org_id uuid)");
            statement.execute("insert into organization values('" + id + "', null)");
            RecordingCodec codec = new RecordingCodec();
            JSqlClient client = JSqlClient.newBuilder()
                    .setDialect(new H2Dialect())
                    .setEntityManager(new EntityManager(Organization.class))
                    .setConnectionManager(ConnectionManager.singleConnectionManager(connection))
                    .setJsonCodec(codec)
                    .build();
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new MicroServiceExporterController(client))
                    .setMessageConverters(new ByteArrayHttpMessageConverter())
                    .build();
            mvc.perform(get(MicroServiceExporterController.BY_IDS)
                            .param("ids", "[\"" + id + "\"]")
                            .param("fetcher", OrganizationFetcher.$.idmOrgId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[{\"id\":\"" + id + "\",\"idmOrgId\":null}]", true));
            assertTrue(codec.read);
            assertTrue(codec.written);
        }
    }

    private static class RecordingCodec extends GsonCodec {

        boolean read;
        boolean written;

        @Override
        protected Object read(String json, Type type) {
            read = true;
            return super.read(json, type);
        }

        @Override
        protected String write(Object value, Type type, boolean pretty) {
            written = true;
            return super.write(value, type, pretty);
        }
    }
}
