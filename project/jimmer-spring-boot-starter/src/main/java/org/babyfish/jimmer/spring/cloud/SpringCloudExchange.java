package org.babyfish.jimmer.spring.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.babyfish.jimmer.impl.util.Classes;
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

    private final ObjectMapper mapper;

    public SpringCloudExchange(RestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    @Override
    public List<ImmutableSpi> findByIds(
            String microServiceName,
            Collection<?> ids,
            Fetcher<?> fetcher
    ) throws JsonProcessingException {
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
                mapper.writeValueAsString(ids),
                fetcher.toString(true)
        );
        return mapper.readValue(
                json,
                mapper.getTypeFactory().constructParametricType(
                        List.class,
                        fetcher.getImmutableType().getJavaClass()
                )
        );
    }

    @Override
    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            String microServiceName,
            ImmutableProp prop,
            Collection<?> targetIds,
            Fetcher<?> fetcher
    ) throws JsonProcessingException {
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
                mapper.writeValueAsString(targetIds),
                fetcher.toString(true)
        );
        TypeFactory typeFactory = mapper.getTypeFactory();
        return mapper.readValue(
                json,
                typeFactory.constructParametricType(
                        List.class,
                        typeFactory.constructParametricType(
                                Tuple2.class,
                                Classes.boxTypeOf(prop.getTargetType().getIdProp().getElementClass()),
                                fetcher.getImmutableType().getJavaClass()
                        )
                )
        );
    }
}
