package org.babyfish.jimmer.spring.cloud;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface MicroServiceExporterAgent {

    String BY_IDS = "/jimmerMicroServiceBridge/byIds";

    String BY_ASSOCIATED_IDS = "/jimmerMicroServiceBridge/byAssociatedIds";

    String IDS = "ids";

    String PROP = "prop";

    String TARGET_IDS = "targetIds";

    String FETCHER = "fetcher";

    @GetMapping(value = BY_IDS)
    void findByIds(
            @RequestParam(IDS) String idArrStr,
            @RequestParam(FETCHER) String fetcherStr,
            HttpServletResponse response
    ) throws Exception;

    @GetMapping(value = BY_ASSOCIATED_IDS)
    void findByAssociatedIds(
            @RequestParam(PROP) String prop,
            @RequestParam(TARGET_IDS) String targetIdArrStr,
            @RequestParam(FETCHER) String fetcherStr,
            HttpServletResponse response
    ) throws Exception;
}
