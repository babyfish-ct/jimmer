package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.jackson.v3.JsonCodecV3;
import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.fastjson2.Fastjson2Codec;
import org.babyfish.jimmer.json.gson.GsonCodec;
import org.babyfish.jimmer.spring.client.OpenApiController;
import org.babyfish.jimmer.spring.client.OpenApiUiController;
import org.babyfish.jimmer.spring.client.TypeScriptController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JsonProviderTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(JimmerJacksonConfig.class);

    @Test
    public void testDefaultWhenBothLibrariesAreInstalled() {
        runner.run(ctx -> assertEquals(JsonCodecV3.class, ctx.getBean(JsonCodec.class).getClass()));
    }

    @Test
    public void testDefaultUsesConfiguredMapperWhenBothLibrariesAreInstalled() {
        runner.withBean(com.fasterxml.jackson.databind.ObjectMapper.class, com.fasterxml.jackson.databind.ObjectMapper::new)
                .run(ctx -> assertEquals(JsonCodecV2.class, ctx.getBean(JsonCodec.class).getClass()));
        runner.withBean(tools.jackson.databind.json.JsonMapper.class, () -> tools.jackson.databind.json.JsonMapper.builder().build())
                .run(ctx -> assertEquals(JsonCodecV3.class, ctx.getBean(JsonCodec.class).getClass()));
    }

    @Test
    public void testApplicationCodecTakesPrecedence() {
        JsonCodec custom = new Fastjson2Codec();
        runner.withBean(JsonCodec.class, () -> custom)
                .run(ctx -> assertSame(custom, ctx.getBean(JsonCodec.class)));
    }

    @Test
    public void testClientMetadataUsesApplicationCodec() {
        JsonCodec codec = spy(new GsonCodec());
        runner.withUserConfiguration(ServletControllerConfiguration.class, PropertiesConfig.class)
                .withPropertyValues(
                        "jimmer.client.ts.path=/ts.zip",
                        "jimmer.client.openapi.path=/openapi.yml",
                        "jimmer.client.openapi.ui-path=/openapi.html"
                )
                .withBean(JsonCodec.class, () -> codec)
                .run(ctx -> {
                    ctx.getBean(TypeScriptController.class).download(null);
                    verify(codec, atLeastOnce()).treeReader();
                    clearInvocations(codec);
                    ctx.getBean(OpenApiController.class).download(null);
                    verify(codec, atLeastOnce()).treeReader();
                    clearInvocations(codec);
                    ctx.getBean(OpenApiUiController.class).download(null).getBody().writeTo(new ByteArrayOutputStream());
                    verify(codec, atLeastOnce()).treeReader();
                });
    }

    @EnableConfigurationProperties(JimmerProperties.class)
    static class PropertiesConfig {}
}
