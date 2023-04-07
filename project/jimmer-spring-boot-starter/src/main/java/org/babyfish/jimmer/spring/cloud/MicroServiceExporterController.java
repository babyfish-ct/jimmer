package org.babyfish.jimmer.spring.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.compiler.FetcherCompiler;
import org.babyfish.jimmer.sql.runtime.MicroServiceExporter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jimmerMicroServiceBridge")
public class MicroServiceExporterController implements MicroServiceExporterAgent {

    private final MicroServiceExporter exporter;

    private final ObjectMapper mapper;

    public MicroServiceExporterController(JSqlClient sqlClient, ObjectMapper mapper) {
        this.exporter = new MicroServiceExporter(sqlClient);
        this.mapper = mapper;
    }

    @GetMapping("/byIds")
    @Override
    public List<ImmutableSpi> findByIds(
            @RequestParam("ids") String idArrStr,
            @RequestParam("fetcher") String fetcherStr
    ) throws JsonProcessingException {
        Fetcher<?> fetcher = FetcherCompiler.compile(fetcherStr);
        Class<?> idType = fetcher.getImmutableType().getIdProp().getElementClass();
        List<?> ids = mapper.readValue(
                idArrStr,
                CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        SimpleType.constructUnsafe(idType)
                )
        );
        return exporter.findByIds(ids, fetcher);
    }

    @GetMapping("/byAssociatedIds")
    @Override
    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            @RequestParam("prop") String prop,
            @RequestParam("targetIds") String targetIdsArrStr,
            @RequestParam("fetcher") String fetcherStr
    ) throws JsonProcessingException {
        Fetcher<?> fetcher = FetcherCompiler.compile(fetcherStr);
        ImmutableProp immutableProp = fetcher.getImmutableType().getProp(prop);
        Class<?> targetIdType = immutableProp.getTargetType().getIdProp().getElementClass();
        List<?> targetIds = mapper.readValue(
                targetIdsArrStr,
                CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        SimpleType.constructUnsafe(targetIdType)
                )
        );
        return exporter.findByAssociatedIds(
                immutableProp,
                targetIds,
                fetcher
        );
    }
}
