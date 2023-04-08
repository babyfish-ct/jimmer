//package org.babyfish.jimmer.example.cloud.book;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.type.CollectionType;
//import com.fasterxml.jackson.databind.type.SimpleType;
//import org.babyfish.jimmer.impl.util.Classes;
//import org.babyfish.jimmer.meta.ImmutableProp;
//import org.babyfish.jimmer.runtime.ImmutableSpi;
//import org.babyfish.jimmer.spring.cloud.MicroServiceExporterAgent;
//import org.babyfish.jimmer.spring.cloud.MicroServiceExporterController;
//import org.babyfish.jimmer.sql.JSqlClient;
//import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
//import org.babyfish.jimmer.sql.fetcher.Fetcher;
//import org.babyfish.jimmer.sql.fetcher.compiler.FetcherCompiler;
//import org.babyfish.jimmer.sql.runtime.MicroServiceExporter;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/jimmerMicroServiceBridge")
//public class TmpExporter implements MicroServiceExporterAgent {
//
//    private final MicroServiceExporterController controller;
//
//    private final MicroServiceExporter exporter;
//
//    public TmpExporter(JSqlClient sqlClient, ObjectMapper mapper) {
//        controller = new MicroServiceExporterController(sqlClient, mapper);
//        exporter = new MicroServiceExporter(sqlClient);
//    }
//
//    @GetMapping("/byIds")
//    public List<ImmutableSpi> findByIds(
//            @RequestParam("ids") String idArrStr,
//            @RequestParam("fetcher") String fetcherStr
//    ) throws Exception {
//        return controller.findByIds(idArrStr, fetcherStr);
//    }
//
//    @GetMapping("/byAssociatedIds")
//    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
//            @RequestParam("prop") String prop,
//            @RequestParam("targetIds") String targetIdsArrStr,
//            @RequestParam("fetcher") String fetcherStr
//    ) throws Exception {
//        Fetcher<?> fetcher = FetcherCompiler.compile(fetcherStr);
//        ImmutableProp immutableProp = fetcher.getImmutableType().getProp(prop);
//        Class<?> targetIdType = immutableProp.getTargetType().getIdProp().getElementClass();
//        List<?> targetIds = new ObjectMapper().readValue(
//                targetIdsArrStr,
//                CollectionType.construct(
//                        List.class,
//                        null,
//                        null,
//                        null,
//                        SimpleType.constructUnsafe(Classes.boxTypeOf(targetIdType))
//                )
//        );
//        return exporter.findByAssociatedIds(
//                immutableProp,
//                targetIds,
//                fetcher
//        );
//    }
//}
