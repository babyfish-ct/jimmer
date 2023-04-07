package org.babyfish.jimmer.spring.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @SuppressWarnings("unchecked")
    @Override
    public List<ImmutableSpi> findByIds(
            String microServiceName,
            Collection<?> ids,
            Fetcher<?> fetcher
    ) throws JsonProcessingException {
        return restTemplate.getForObject(
                "http://{microServiceName}/jimmerMicroServiceBridge/byIds" +
                        "?ids={ids}&fetcher={fetcher}",
                List.class,
                microServiceName,
                mapper.writeValueAsString(ids),
                fetcher.toString(true)
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            String microServiceName,
            ImmutableProp prop,
            Collection<?> targetIds,
            Fetcher<?> fetcher
    ) throws JsonProcessingException {
        return restTemplate.getForObject(
                "http://{microServiceName}/jimmerMicroServiceBridge/byAssociatedIds" +
                        "?prop={prop}&targetIds={targetIds}&fetcher={fetcher}",
                List.class,
                microServiceName,
                prop.getName(),
                mapper.writeValueAsString(targetIds),
                fetcher.toString(true)
        );
    }
}
