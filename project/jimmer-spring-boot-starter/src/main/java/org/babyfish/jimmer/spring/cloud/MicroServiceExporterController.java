package org.babyfish.jimmer.spring.cloud;

import io.swagger.v3.oas.annotations.Hidden;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.compiler.FetcherCompiler;
import org.babyfish.jimmer.sql.runtime.MicroServiceExporter;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Hidden
@ApiIgnore
@RestController
@Transactional(readOnly = true)
public class MicroServiceExporterController {

    public static final String BY_IDS = "/jimmerMicroServiceBridge/byIds";

    public static final String BY_ASSOCIATED_IDS = "/jimmerMicroServiceBridge/byAssociatedIds";

    public static final String IDS = "ids";

    public static final String PROP = "prop";

    public static final String TARGET_IDS = "targetIds";

    public static final String FETCHER = "fetcher";

    private final MicroServiceExporter exporter;

    private final JsonCodec<?> jsonCodec;

    public MicroServiceExporterController(JSqlClient sqlClient) {
        this.exporter = new MicroServiceExporter(sqlClient);
        this.jsonCodec = sqlClient.getJsonCodec();
    }

    @GetMapping(value = BY_IDS, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ImmutableSpi> findByIds(
            @RequestParam(IDS) String idArrStr,
            @RequestParam(FETCHER) String fetcherStr
    ) throws Exception {
        Fetcher<?> fetcher = FetcherCompiler.compile(fetcherStr);
        Class<?> idType = fetcher.getImmutableType().getIdProp().getElementClass();
        List<?> ids = jsonCodec
                .readerForListOf(idType)
                .read(idArrStr);
        return exporter.findByIds(ids, fetcher);
    }

    @GetMapping(value = BY_ASSOCIATED_IDS, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            @RequestParam(PROP) String prop,
            @RequestParam(TARGET_IDS) String targetIdArrStr,
            @RequestParam(FETCHER) String fetcherStr
    ) throws Exception {
        Fetcher<?> fetcher = FetcherCompiler.compile(fetcherStr);
        ImmutableProp immutableProp = fetcher.getImmutableType().getProp(prop);
        Class<?> targetIdType = immutableProp.getTargetType().getIdProp().getElementClass();
        List<?> targetIds = jsonCodec
                .<List<?>>readerFor(tf -> tf.constructListType(targetIdType))
                .read(targetIdArrStr);

        return exporter.findByAssociatedIds(
                immutableProp,
                targetIds,
                fetcher
        );
    }
}
