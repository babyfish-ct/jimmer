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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Controller
public class MicroServiceExporterController implements MicroServiceExporterAgent {

    private final MicroServiceExporter exporter;

    private final ObjectMapper mapper;

    public MicroServiceExporterController(JSqlClient sqlClient, ObjectMapper mapper) {
        this.exporter = new MicroServiceExporter(sqlClient);
        this.mapper = mapper;
    }

    @GetMapping(value = BY_IDS)
    public void findByIds(
            @RequestParam(IDS) String idArrStr,
            @RequestParam(FETCHER) String fetcherStr,
            HttpServletResponse response
    ) throws JsonProcessingException, IOException {
        Fetcher<?> fetcher = FetcherCompiler.compile(fetcherStr);
        Class<?> idType = fetcher.getImmutableType().getIdProp().getElementClass();
        List<?> ids = mapper.readValue(
                idArrStr,
                CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        SimpleType.constructUnsafe(Classes.boxTypeOf(idType))
                )
        );
        List<ImmutableSpi> data = exporter.findByIds(ids, fetcher);
        response.setContentType("application/json");
        try (OutputStream out = response.getOutputStream()) {
            mapper.writeValue(out, data);
        }
    }

    @GetMapping(value = BY_ASSOCIATED_IDS)
    public void findByAssociatedIds(
            @RequestParam(PROP) String prop,
            @RequestParam(TARGET_IDS) String targetIdArrStr,
            @RequestParam(FETCHER) String fetcherStr,
            HttpServletResponse response
    ) throws Exception {
        Fetcher<?> fetcher = FetcherCompiler.compile(fetcherStr);
        ImmutableProp immutableProp = fetcher.getImmutableType().getProp(prop);
        Class<?> targetIdType = immutableProp.getTargetType().getIdProp().getElementClass();
        List<?> targetIds = mapper.readValue(
                targetIdArrStr,
                CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        SimpleType.constructUnsafe(Classes.boxTypeOf(targetIdType))
                )
        );
        List<Tuple2<Object, ImmutableSpi>> data = exporter.findByAssociatedIds(
                immutableProp,
                targetIds,
                fetcher
        );
        response.setContentType("application/json");
        try (OutputStream out = response.getOutputStream()) {
            mapper.writeValue(out, data);
        }
    }
}
