package org.babyfish.jimmer.spring.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.compiler.FetcherCompiler;
import org.babyfish.jimmer.sql.runtime.MicroServiceExporter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(MicroServiceExporterAgent.JIMMER_MICRO_SERVICE_BRIDGE)
public class MicroServiceExporterController implements MicroServiceExporterAgent {

    private final MicroServiceExporter exporter;

    private final ObjectMapper mapper;

    public MicroServiceExporterController(JSqlClient sqlClient, ObjectMapper mapper) {
        this.exporter = new MicroServiceExporter(sqlClient);
        this.mapper = mapper;
    }

    @PostMapping(value = BY_IDS, produces="application/json")
    @Override
    public List<ImmutableSpi> findByIds(
            @RequestBody FindByIdsRequest request
    ) throws JsonProcessingException {
        Fetcher<?> fetcher = FetcherCompiler.compile(request.getFetcherStr());
        Class<?> idType = fetcher.getImmutableType().getIdProp().getElementClass();
        List<?> ids = mapper.readValue(
                request.getIdArrStr(),
                CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        SimpleType.constructUnsafe(Classes.boxTypeOf(idType))
                )
        );
        return exporter.findByIds(ids, fetcher);
    }

    @PostMapping(value = BY_ASSOCIATED_IDS, produces="application/json")
    @Override
    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            @RequestBody FindByAssociatedIdsRequest request
    ) throws JsonProcessingException {
        Fetcher<?> fetcher = FetcherCompiler.compile(request.getFetcherStr());
        ImmutableProp immutableProp = fetcher.getImmutableType().getProp(request.getProp());
        Class<?> targetIdType = immutableProp.getTargetType().getIdProp().getElementClass();
        List<?> targetIds = mapper.readValue(
                request.getTargetIdArrStr(),
                CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        SimpleType.constructUnsafe(Classes.boxTypeOf(targetIdType))
                )
        );
        return exporter.findByAssociatedIds(
                immutableProp,
                targetIds,
                fetcher
        );
    }
}
