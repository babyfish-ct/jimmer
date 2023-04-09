package org.babyfish.jimmer.spring.cloud;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(MicroServiceExporterAgent.JIMMER_MICRO_SERVICE_BRIDGE)
public interface MicroServiceExporterAgent {

    String JIMMER_MICRO_SERVICE_BRIDGE = "/jimmerMicroServiceBridge";

    String BY_IDS = "/byIds";

    String BY_ASSOCIATED_IDS = "/byAssociatedIds";

    @PostMapping(value = BY_IDS, produces="application/json")
    List<ImmutableSpi> findByIds(
            @RequestBody FindByIdsRequest request
    ) throws Exception;

    @PostMapping(value = BY_ASSOCIATED_IDS, produces="application/json")
    List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            @RequestBody FindByAssociatedIdsRequest request
    ) throws Exception;
}
