package org.babyfish.jimmer.spring.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import org.babyfish.jimmer.impl.util.Classes;
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
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.util.List;

import static org.babyfish.jimmer.spring.cloud.MicroServiceExporterController.*;

@Hidden
@ApiIgnore
@RestController
@Transactional(readOnly = true)
public class JakartaMicroServiceExporterController {
    private final MicroServiceExporter exporter;

    private final ObjectMapper mapper;

    public JakartaMicroServiceExporterController(JSqlClient sqlClient, ObjectMapper mapper) {
        this.exporter = new MicroServiceExporter(sqlClient);
        this.mapper = mapper;
    }

    @GetMapping(value = BY_IDS, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ImmutableSpi> findByIds(
            @RequestParam(IDS) String idArrStr,
            @RequestParam(FETCHER) String fetcherStr
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
        return exporter.findByIds(ids, fetcher);
    }

    @GetMapping(value = BY_ASSOCIATED_IDS, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
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
        return exporter.findByAssociatedIds(
                immutableProp,
                targetIds,
                fetcher
        );
    }
}
