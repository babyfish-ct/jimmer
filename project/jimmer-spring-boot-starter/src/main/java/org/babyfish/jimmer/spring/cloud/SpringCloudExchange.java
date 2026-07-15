package org.babyfish.jimmer.spring.cloud;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.json.codec.JsonCodec;
import org.babyfish.jimmer.json.codec.JsonType;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.MicroServiceExchange;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;

public class SpringCloudExchange implements MicroServiceExchange {

    private final RestTemplate restTemplate;

    private final JsonCodec jsonCodec;

    public SpringCloudExchange(RestTemplate restTemplate, JsonCodec jsonCodec) {
        this.restTemplate = restTemplate;
        this.jsonCodec = jsonCodec;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ImmutableSpi> findByIds(
            String microServiceName,
            Collection<?> ids,
            Fetcher<?> fetcher
    ) throws Exception {
        String json = restTemplate.getForObject(
                "http://" +
                        microServiceName +
                        MicroServiceExporterController.BY_IDS +
                        "?" +
                        MicroServiceExporterController.IDS +
                        "={ids}&" +
                        MicroServiceExporterController.FETCHER +
                        "={fetcher}",
                String.class,
                jsonCodec.writer().writeAsString(ids),
                fetcher.toString()
        );

        return (List<ImmutableSpi>) jsonCodec
                .readerFor(JsonType.listOf(fetcher.getImmutableType().getJavaClass()))
                .read(json);
    }

    @Override
    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            String microServiceName,
            ImmutableProp prop,
            Collection<?> targetIds,
            Fetcher<?> fetcher
    ) throws Exception {
        String json = restTemplate.getForObject(
                "http://" +
                        microServiceName +
                        MicroServiceExporterController.BY_ASSOCIATED_IDS +
                        "?" +
                        MicroServiceExporterController.PROP +
                        "={prop}&" +
                        MicroServiceExporterController.TARGET_IDS +
                        "={targetIds}&" +
                        MicroServiceExporterController.FETCHER +
                        "={fetcher}",
                String.class,
                prop.getName(),
                jsonCodec.writer().writeAsString(targetIds),
                fetcher.toString()
        );

        return jsonCodec
                .<List<Tuple2<Object, ImmutableSpi>>>readerFor(JsonType.collectionOf(
                        List.class,
                        JsonType.parameterized(
                                Tuple2.class,
                                Classes.boxTypeOf(prop.getTargetType().getIdProp().getElementClass()),
                                fetcher.getImmutableType().getJavaClass())))
                .read(json);
    }
}
