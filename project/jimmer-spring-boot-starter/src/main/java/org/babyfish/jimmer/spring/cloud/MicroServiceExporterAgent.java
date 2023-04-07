package org.babyfish.jimmer.spring.cloud;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping("/jimmerMicroServiceBridge")
public interface MicroServiceExporterAgent {

    @GetMapping("/byIds")
    List<ImmutableSpi> findByIds(
            @RequestParam("ids") String idArrStr,
            @RequestParam("fetcher") String fetcherStr
    ) throws Exception;

    @GetMapping("/byAssociatedIds")
    List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            @RequestParam("prop") String prop,
            @RequestParam("targetIds") String targetIdsArrStr,
            @RequestParam("fetcher") String fetcherStr
    ) throws Exception;
}
